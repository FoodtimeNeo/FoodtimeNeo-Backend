package com.FoodtimeNeo.venue;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.mapper.AiModerationStore;
import com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.moderation.dto.*;
import com.FoodtimeNeo.review.dto.ReviewLikeResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.venue.VenueModels.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * Restaurant/stall reviews share a normalized version model. Public queries never join a pending
 * version.
 */
@Service
public class VenueReviewService {
  private final JdbcTemplate db;
  private final ApplicationEventPublisher events;
  private final AiModerationStore ai;
  private final AiTriageProperties policy;

  public VenueReviewService(
      JdbcTemplate db,
      ApplicationEventPublisher events,
      AiModerationStore ai,
      AiTriageProperties policy) {
    this.db = db;
    this.events = events;
    this.ai = ai;
    this.policy = policy;
  }

  public static boolean isType(String type) {
    return Set.of("hall-review", "stall-review", "hall-review-reply", "stall-review-reply")
        .contains(type);
  }

  private static final String CONTEXT =
      """
      JOIN foodtime.venue_review_targets t ON t.id=r.target_id
      LEFT JOIN foodtime.stalls st ON st.id=t.stall_id
      JOIN foodtime.dining_halls h ON h.id=COALESCE(t.dining_hall_id,st.dining_hall_id)
      """;
  private static final String FIELDS =
      """
      CASE WHEN t.stall_id IS NULL THEN 'hall' ELSE 'stall' END AS target_type,
      COALESCE(t.stall_id,t.dining_hall_id) AS target_object,COALESCE(st.name,h.name) AS target_name
      """;
  private static final String REVIEW =
      """
      SELECT r.id,r.id AS review_id,s.id AS submission_id,r.user_id,u.display_name,
      s.rating,s.content,CASE WHEN s.invalidated_at IS NULL THEN s.status ELSE 'invalidated' END AS status,s.review_reason,
      CASE WHEN s.id=r.current_submission_id THEN r.first_published_at ELSE s.submitted_at END AS created_at,
      r.updated_at,(r.user_id=?) AS owned_by_me,
      z.like_count,z.reply_count,z.counter_version,(l.user_id IS NOT NULL) AS liked_by_me,
      NULL::uuid AS reply_to_id,NULL::text AS reply_to_name,false AS reply_to_deleted,r.publication_version,
      """
          + FIELDS
          + """
          FROM foodtime.venue_reviews r JOIN foodtime.venue_review_submissions s ON s.review_id=r.id
          JOIN foodtime.users u ON u.id=r.user_id
          JOIN foodtime.venue_review_stats z ON z.review_id=r.id
          LEFT JOIN foodtime.venue_review_likes l ON l.review_id=r.id AND l.user_id=?
          """
          + CONTEXT;
  private static final String REPLY =
      """
      SELECT c.id,r.id AS review_id,NULL::uuid AS submission_id,c.user_id,u.display_name,
      NULL::integer AS rating,c.content,c.status,c.review_reason,c.created_at,c.updated_at,
      (c.user_id=?) AS owned_by_me,z.like_count,0::bigint AS reply_count,z.counter_version,
      (l.user_id IS NOT NULL) AS liked_by_me,c.reply_to_id,tu.display_name AS reply_to_name,
      (target.deleted_at IS NOT NULL) AS reply_to_deleted,r.publication_version,
      """
          + FIELDS
          + """
          FROM foodtime.venue_review_replies c JOIN foodtime.venue_reviews r ON r.id=c.review_id
          JOIN foodtime.users u ON u.id=c.user_id JOIN foodtime.venue_review_reply_stats z ON z.reply_id=c.id
          LEFT JOIN foodtime.venue_review_reply_likes l ON l.reply_id=c.id AND l.user_id=?
          LEFT JOIN foodtime.venue_review_replies target ON target.id=c.reply_to_id
          LEFT JOIN foodtime.users tu ON tu.id=target.user_id
          """
          + CONTEXT;

  private static Instant time(ResultSet r, String field) throws SQLException {
    Timestamp t = r.getTimestamp(field);
    return t == null ? null : t.toInstant();
  }

  private static final RowMapper<Row> ROW =
      (r, n) ->
          new Row(
              r.getObject("id", UUID.class),
              r.getObject("review_id", UUID.class),
              r.getObject("submission_id", UUID.class),
              r.getObject("user_id", UUID.class),
              r.getString("display_name"),
              r.getString("target_type"),
              r.getObject("target_object", UUID.class),
              r.getString("target_name"),
              r.getObject("rating", Integer.class),
              r.getString("content"),
              r.getString("status"),
              r.getBoolean("owned_by_me") ? r.getString("review_reason") : null,
              time(r, "created_at"),
              time(r, "updated_at"),
              r.getBoolean("owned_by_me"),
              r.getLong("like_count"),
              r.getLong("reply_count"),
              r.getBoolean("liked_by_me"),
              r.getLong("counter_version"),
              r.getObject("reply_to_id", UUID.class),
              r.getString("reply_to_name"),
              r.getBoolean("reply_to_deleted"),
              r.getLong("publication_version"));

  private record Identity(
      UUID id, UUID target, UUID user, UUID current, long version, Instant deleted) {}

  private Identity identity(UUID id, boolean locked) {
    var rows =
        db.query(
            "SELECT * FROM foodtime.venue_reviews WHERE id=?" + (locked ? " FOR UPDATE" : ""),
            (r, n) ->
                new Identity(
                    r.getObject("id", UUID.class),
                    r.getObject("target_id", UUID.class),
                    r.getObject("user_id", UUID.class),
                    r.getObject("current_submission_id", UUID.class),
                    r.getLong("publication_version"),
                    time(r, "deleted_at")),
            id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  private UUID targetId(String type, UUID object) {
    if (!Set.of("hall", "stall").contains(type)) throw bad("评价类型不合法");
    var ids =
        db.query(
            "SELECT id FROM foodtime.venue_review_targets WHERE "
                + (type.equals("hall") ? "dining_hall_id" : "stall_id")
                + "=?",
            (r, n) -> r.getObject(1, UUID.class),
            object);
    if (ids.isEmpty()) throw missing();
    return ids.get(0);
  }

  /** Every write locks the parent catalog before the review and then the child submission/reply. */
  private void catalog(UUID target, boolean lock, boolean visible) {
    var refs =
        db.query(
            "SELECT t.dining_hall_id,t.stall_id,st.dining_hall_id AS parent FROM"
                + " foodtime.venue_review_targets t LEFT JOIN foodtime.stalls st ON"
                + " st.id=t.stall_id WHERE t.id=?",
            (r, n) ->
                new UUID[] {
                  r.getObject("dining_hall_id", UUID.class),
                  r.getObject("stall_id", UUID.class),
                  r.getObject("parent", UUID.class)
                },
            target);
    if (refs.isEmpty()) throw missing();
    UUID[] ref = refs.get(0);
    UUID hall = ref[0] == null ? ref[2] : ref[0];
    var hs =
        db.query(
            "SELECT status FROM foodtime.dining_halls WHERE id=?" + (lock ? " FOR SHARE" : ""),
            (r, n) -> r.getString(1),
            hall);
    if (hs.isEmpty() || visible && !hs.get(0).equals("active")) throw missing();
    if (ref[1] != null) {
      var ss =
          db.query(
              "SELECT status FROM foodtime.stalls WHERE id=?" + (lock ? " FOR SHARE" : ""),
              (r, n) -> r.getString(1),
              ref[1]);
      if (ss.isEmpty() || visible && !ss.get(0).equals("active")) throw missing();
    }
  }

  private Identity lockReview(UUID id, boolean visible) {
    Identity before = identity(id, false);
    if (before == null) throw missing();
    catalog(before.target(), true, visible);
    Identity row = identity(id, true);
    if (visible && (row.deleted() != null || row.current() == null)) throw missing();
    return row;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  public Target target(String type, UUID object) {
    UUID target = targetId(type, object);
    catalog(target, false, true);
    return summary(target);
  }

  private Target summary(UUID target) {
    var buckets =
        db.query(
            "SELECT rating,review_count FROM foodtime.venue_rating_stats WHERE target_id=? ORDER BY"
                + " rating",
            (r, n) -> new Bucket(r.getInt(1), r.getLong(2)),
            target);
    return db.queryForObject(
        """
        SELECT t.id,t.stall_id,h.id AS hall_id,h.name AS hall_name,st.name AS stall_name,
        v.average_score,v.review_count FROM foodtime.venue_review_targets t
        LEFT JOIN foodtime.stalls st ON st.id=t.stall_id JOIN foodtime.dining_halls h ON h.id=COALESCE(t.dining_hall_id,st.dining_hall_id)
        JOIN foodtime.venue_rating_summary v ON v.target_id=t.id WHERE t.id=?
        """,
        (r, n) ->
            new Target(
                target,
                r.getObject("stall_id") == null ? "hall" : "stall",
                r.getObject("stall_id") == null
                    ? r.getObject("hall_id", UUID.class)
                    : r.getObject("stall_id", UUID.class),
                r.getString("stall_name") == null
                    ? r.getString("hall_name")
                    : r.getString("stall_name"),
                r.getObject("hall_id", UUID.class),
                r.getString("hall_name"),
                r.getObject("stall_id", UUID.class),
                r.getString("stall_name"),
                r.getBigDecimal("average_score"),
                r.getLong("review_count"),
                buckets),
        target);
  }

  private String filter(String rating) {
    return switch (rating == null ? "all" : rating) {
      case "all" -> "";
      case "5" -> " AND s.rating=5";
      case "4" -> " AND s.rating=4";
      case "1-3" -> " AND s.rating BETWEEN 1 AND 3";
      default -> throw bad("评分筛选无效");
    };
  }

  private record Cursor(Instant time, UUID id) {}

  private Cursor cursor(String value, String scope) {
    if (value == null || value.isBlank()) return null;
    try {
      if (value.length() > 512) throw new IllegalArgumentException();
      String[] p =
          new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", -1);
      if (p.length != 3 || !p[0].equals(scope)) throw new IllegalArgumentException();
      Instant time = Instant.parse(p[1]);
      if (time.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
          || time.isAfter(Instant.parse("9999-12-31T23:59:59Z")))
        throw new IllegalArgumentException();
      return new Cursor(time, UUID.fromString(p[2]));
    } catch (RuntimeException e) {
      throw bad("分页游标无效");
    }
  }

  private String next(String scope, Instant time, UUID id) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString((scope + "|" + time + "|" + id).getBytes(StandardCharsets.UTF_8));
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  public CursorPageResponse<Row> list(
      String type, UUID object, UUID viewer, int size, String after, String rating) {
    new PageRequest(1, size);
    UUID target = targetId(type, object);
    catalog(target, false, true);
    String predicate = filter(rating), scope = target + ":" + (rating == null ? "all" : rating);
    Cursor c = cursor(after, scope);
    var args = new ArrayList<Object>(Arrays.asList(viewer, viewer, target));
    String seek = "";
    if (c != null) {
      seek = " AND (r.first_published_at,r.id)<(?,?)";
      args.add(Timestamp.from(c.time()));
      args.add(c.id());
    }
    args.add(size + 1);
    var rows =
        db.query(
            REVIEW
                + " WHERE r.target_id=? AND r.deleted_at IS NULL AND s.id=r.current_submission_id"
                + " AND s.status='approved'"
                + predicate
                + seek
                + " ORDER BY r.first_published_at DESC,r.id DESC LIMIT ?",
            ROW,
            args.toArray());
    long total =
        summary(target).distribution().stream()
            .filter(
                b ->
                    rating == null
                        || rating.equals("all")
                        || rating.equals("1-3") && b.rating() <= 3
                        || rating.equals(Integer.toString(b.rating())))
            .mapToLong(Bucket::reviewCount)
            .sum();
    boolean more = rows.size() > size;
    var items = List.copyOf(rows.subList(0, Math.min(size, rows.size())));
    Row last = items.isEmpty() ? null : items.get(items.size() - 1);
    return new CursorPageResponse<>(
        items, total, more, more ? next(scope, last.createdAt(), last.id()) : null);
  }

  private Row submission(UUID id, UUID viewer) {
    var rows = db.query(REVIEW + " WHERE s.id=? AND r.user_id=?", ROW, viewer, viewer, id, viewer);
    return rows.isEmpty() ? null : rows.get(0);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  public Mine mine(String type, UUID object, UUID user) {
    UUID target = targetId(type, object);
    catalog(target, false, true);
    var ids =
        db.query(
            "SELECT id FROM foodtime.venue_reviews WHERE target_id=? AND user_id=? AND deleted_at"
                + " IS NULL",
            (r, n) -> r.getObject(1, UUID.class),
            target,
            user);
    if (ids.isEmpty()) return new Mine(null, null, null);
    Identity root = identity(ids.get(0), false);
    var pending =
        db.query(
            REVIEW
                + " WHERE r.id=? AND s.status IN ('pending','rejected') AND s.invalidated_at IS"
                + " NULL AND s.base_version>=r.publication_version ORDER BY s.submitted_at"
                + " DESC,s.id DESC LIMIT 1",
            ROW,
            user,
            user,
            root.id());
    Row privateRow = pending.isEmpty() ? null : pending.get(0);
    return new Mine(
        root.id(), root.current() == null ? null : submission(root.current(), user), privateRow);
  }

  @Transactional(readOnly = true)
  public Row status(UUID id, UUID user, boolean reply) {
    Row row = reply ? reply(id, user) : submission(id, user);
    if (row == null || !row.ownedByMe()) throw missing();
    return row;
  }

  private Row replay(UUID request, UUID user, UUID target, int rating, String content) {
    var ids =
        db.query(
            "SELECT s.id,r.user_id,r.target_id,s.rating,s.content FROM"
                + " foodtime.venue_review_submissions s JOIN foodtime.venue_reviews r ON"
                + " r.id=s.review_id WHERE s.client_request_id=?",
            (r, n) ->
                new Object[] {
                  r.getObject(1, UUID.class),
                  r.getObject(2, UUID.class),
                  r.getObject(3, UUID.class),
                  r.getInt(4),
                  r.getString(5)
                },
            request);
    if (ids.isEmpty()) return null;
    Object[] p = ids.get(0);
    if (!user.equals(p[1])
        || !target.equals(p[2])
        || !Integer.valueOf(rating).equals(p[3])
        || !content.equals(p[4])) throw conflict("同一操作ID不能用于不同评价");
    return submission((UUID) p[0], user);
  }

  @Transactional
  public Row submit(String type, UUID object, UUID user, ReviewInput body) {
    String content = text(body.content());
    int rating;
    try {
      rating = body.rating().intValueExact();
    } catch (ArithmeticException e) {
      throw bad("请选择1至5的整数评分");
    }
    if (rating < 1 || rating > 5) throw bad("请选择1至5的整数评分");
    UUID target = targetId(type, object);
    Row prior = replay(body.clientRequestId(), user, target, rating, content);
    if (prior != null) return prior;
    catalog(target, true, true);
    db.update(
        "INSERT INTO foodtime.venue_reviews(id,target_id,user_id) VALUES(?,?,?) ON"
            + " CONFLICT(target_id,user_id) WHERE deleted_at IS NULL DO NOTHING",
        UUID.randomUUID(),
        target,
        user);
    UUID rootId =
        db.queryForObject(
            "SELECT id FROM foodtime.venue_reviews WHERE target_id=? AND user_id=? AND deleted_at"
                + " IS NULL FOR UPDATE",
            UUID.class,
            target,
            user);
    Identity root = identity(rootId, false);
    prior = replay(body.clientRequestId(), user, target, rating, content);
    if (prior != null) return prior;
    if (Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM foodtime.venue_review_submissions WHERE review_id=? AND"
                + " status='pending' AND invalidated_at IS NULL)",
            Boolean.class,
            rootId))) throw conflict("已有一条待审核评价，请等待审核完成");
    UUID id = UUID.randomUUID();
    int written =
        db.update(
            "INSERT INTO"
                + " foodtime.venue_review_submissions(id,review_id,client_request_id,base_version,rating,content)"
                + " VALUES(?,?,?,?,?,?) ON CONFLICT(client_request_id) DO NOTHING",
            id,
            rootId,
            body.clientRequestId(),
            root.version(),
            rating,
            content);
    if (written == 0)
      return Objects.requireNonNull(replay(body.clientRequestId(), user, target, rating, content));
    events.publishEvent(new SubmissionDiagnoseEvent(type + "-review", id));
    return submission(id, user);
  }

  @Transactional
  public void deleteReview(UUID id, UUID user, boolean admin, String reason) {
    Identity root = lockReview(id, false);
    if (!admin && !root.user().equals(user)) throw missing();
    if (root.deleted() != null) return;
    if (root.current() == null) throw missing();
    if (admin) reason = text(reason);
    db.update(
        "UPDATE foodtime.venue_review_submissions SET invalidated_at=now() WHERE review_id=? AND"
            + " status='pending' AND invalidated_at IS NULL",
        id);
    db.update("UPDATE foodtime.venue_reviews SET deleted_at=now() WHERE id=?", id);
    if (admin) log(root.current(), false, user, null, "remove", reason);
  }

  private Row reply(UUID id, UUID viewer) {
    var rows = db.query(REPLY + " WHERE c.id=? AND c.deleted_at IS NULL", ROW, viewer, viewer, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  @Transactional
  public Row submitReply(UUID review, UUID user, ReplyInput body) {
    String content = text(body.content());
    var prior =
        db.query(
            "SELECT id,review_id,reply_to_id,content FROM foodtime.venue_review_replies WHERE"
                + " user_id=? AND client_request_id=?",
            (r, n) ->
                new Object[] {
                  r.getObject(1, UUID.class),
                  r.getObject(2, UUID.class),
                  r.getObject(3, UUID.class),
                  r.getString(4)
                },
            user,
            body.clientRequestId());
    if (!prior.isEmpty()) {
      Object[] p = prior.get(0);
      if (!review.equals(p[1]) || !Objects.equals(body.replyToId(), p[2]) || !content.equals(p[3]))
        throw conflict("同一操作ID不能用于不同回复");
      Row row = reply((UUID) p[0], user);
      if (row == null) throw missing();
      return row;
    }
    Identity root = lockReview(review, true);
    if (body.replyToId() != null
        && !Boolean.TRUE.equals(
            db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.venue_review_replies WHERE id=? AND"
                    + " review_id=? AND status='approved' AND deleted_at IS NULL)",
                Boolean.class,
                body.replyToId(),
                review))) throw missing();
    // Parent lock also serializes duplicate requests for the same floor.
    var raced =
        db.query(
            "SELECT id FROM foodtime.venue_review_replies WHERE user_id=? AND client_request_id=?",
            (r, n) -> r.getObject(1, UUID.class),
            user,
            body.clientRequestId());
    if (!raced.isEmpty()) return submitReply(review, user, body);
    UUID id = UUID.randomUUID();
    if (db.update(
            "INSERT INTO"
                + " foodtime.venue_review_replies(id,review_id,reply_to_id,user_id,client_request_id,content)"
                + " VALUES(?,?,?,?,?,?) ON CONFLICT(user_id,client_request_id) DO NOTHING",
            id,
            review,
            body.replyToId(),
            user,
            body.clientRequestId(),
            content)
        == 0) return submitReply(review, user, body);
    events.publishEvent(new SubmissionDiagnoseEvent(type(root.target()) + "-review-reply", id));
    return reply(id, user);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  public CursorPageResponse<Row> replies(
      UUID review, UUID viewer, int size, String after, boolean mine) {
    new PageRequest(1, size);
    Identity root = identity(review, false);
    if (root == null || root.deleted() != null || root.current() == null) throw missing();
    catalog(root.target(), false, true);
    String scope = review + (mine ? ":mine:" + viewer : "");
    Cursor c = cursor(after, scope);
    var args = new ArrayList<Object>(Arrays.asList(viewer, viewer, review));
    String where =
        " WHERE c.review_id=? AND c.deleted_at IS NULL"
            + (mine
                ? " AND c.user_id=? AND c.status IN ('pending','rejected')"
                : " AND c.status='approved'");
    if (mine) args.add(viewer);
    if (c != null) {
      where += " AND (c.created_at,c.id)>(?,?)";
      args.add(Timestamp.from(c.time()));
      args.add(c.id());
    }
    args.add(size + 1);
    var rows = db.query(REPLY + where + " ORDER BY c.created_at,c.id LIMIT ?", ROW, args.toArray());
    boolean more = rows.size() > size;
    var items = List.copyOf(rows.subList(0, Math.min(size, rows.size())));
    Row last = items.isEmpty() ? null : items.get(items.size() - 1);
    long total =
        mine
            ? db.queryForObject(
                "SELECT count(*) FROM foodtime.venue_review_replies WHERE review_id=? AND user_id=?"
                    + " AND status IN ('pending','rejected') AND deleted_at IS NULL",
                Long.class,
                review,
                viewer)
            : db.queryForObject(
                "SELECT reply_count FROM foodtime.venue_review_stats WHERE review_id=?",
                Long.class,
                review);
    return new CursorPageResponse<>(
        items, total, more, more ? next(scope, last.createdAt(), last.id()) : null);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  public PageResponse<Row> personal(UUID user, String type, boolean replies, PageRequest page) {
    if (type != null && !Set.of("hall", "stall").contains(type)) throw bad("评价类型不合法");
    String scope =
        type == null
            ? ""
            : type.equals("hall") ? " AND t.stall_id IS NULL" : " AND t.stall_id IS NOT NULL";
    String where =
        replies
            ? " WHERE c.user_id=? AND c.deleted_at IS NULL"
            : " WHERE r.user_id=? AND r.deleted_at IS NULL AND s.id=(SELECT v.id FROM"
                  + " foodtime.venue_review_submissions v WHERE v.review_id=r.id ORDER BY"
                  + " v.submitted_at DESC,v.id DESC LIMIT 1)";
    String query = (replies ? REPLY : REVIEW) + where + scope;
    var items =
        db.query(
            query
                + (replies
                    ? " ORDER BY c.created_at DESC,c.id DESC"
                    : " ORDER BY s.submitted_at DESC,s.id DESC")
                + " LIMIT ? OFFSET ?",
            ROW,
            user,
            user,
            user,
            page.size(),
            page.offset());
    long total =
        db.queryForObject("SELECT count(*) FROM (" + query + ") q", Long.class, user, user, user);
    return PageResponse.of(items, total, page);
  }

  @Transactional
  public void deleteReply(UUID id, UUID user, boolean admin, String reason) {
    Row before = reply(id, user);
    if (before == null) {
      if (Boolean.TRUE.equals(
          db.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM foodtime.venue_review_replies WHERE id=? AND user_id=?"
                  + " AND deleted_at IS NOT NULL)",
              Boolean.class,
              id,
              user))) return;
      throw missing();
    }
    if (!admin && !before.ownedByMe()) throw missing();
    lockReview(before.reviewId(), false);
    var locked =
        db.query(
            "SELECT id FROM foodtime.venue_review_replies WHERE id=? AND deleted_at IS NULL FOR"
                + " UPDATE",
            (r, n) -> r.getObject(1, UUID.class),
            id);
    if (locked.isEmpty()) return;
    if (admin) reason = text(reason);
    db.update("UPDATE foodtime.venue_review_replies SET deleted_at=now() WHERE id=?", id);
    if (admin) log(id, true, user, null, "remove", reason);
  }

  @Transactional
  public ReviewLikeResponse like(UUID id, UUID user, boolean liked, boolean reply) {
    UUID rootId = id;
    if (reply) {
      var ids =
          db.query(
              "SELECT review_id FROM foodtime.venue_review_replies WHERE id=?",
              (r, n) -> r.getObject(1, UUID.class),
              id);
      if (ids.isEmpty()) throw missing();
      rootId = ids.get(0);
    }
    lockReview(rootId, liked);
    if (reply) {
      var states =
          db.query(
              "SELECT status,deleted_at FROM foodtime.venue_review_replies WHERE id=? FOR UPDATE",
              (r, n) -> new Object[] {r.getString(1), r.getTimestamp(2)},
              id);
      if (states.isEmpty()
          || liked && (!states.get(0)[0].equals("approved") || states.get(0)[1] != null))
        throw missing();
    }
    String table = reply ? "foodtime.venue_review_reply_likes" : "foodtime.venue_review_likes",
        key = reply ? "reply_id" : "review_id",
        stats = reply ? "foodtime.venue_review_reply_stats" : "foodtime.venue_review_stats";
    if (liked)
      db.update(
          "INSERT INTO " + table + "(" + key + ",user_id) VALUES(?,?) ON CONFLICT DO NOTHING",
          id,
          user);
    else db.update("DELETE FROM " + table + " WHERE " + key + "=? AND user_id=?", id, user);
    return db.queryForObject(
        "SELECT s.like_count,s.counter_version,EXISTS(SELECT 1 FROM "
            + table
            + " l WHERE l."
            + key
            + "=s."
            + key
            + " AND l.user_id=?) AS liked FROM "
            + stats
            + " s WHERE s."
            + key
            + "=?",
        (r, n) ->
            new ReviewLikeResponse(
                r.getBoolean("liked"), r.getLong("like_count"), r.getLong("counter_version")),
        user,
        id);
  }

  private record Decision(UUID id, UUID review, String status, Long base, Instant invalidated) {}

  private Decision decision(UUID id, boolean reply, boolean lock) {
    var rows =
        db.query(
            "SELECT id,review_id,status,"
                + (reply
                    ? "NULL::bigint AS base_version,NULL::timestamptz AS invalidated_at"
                    : "base_version,invalidated_at")
                + " FROM "
                + (reply ? "foodtime.venue_review_replies" : "foodtime.venue_review_submissions")
                + " WHERE id=?"
                + (reply ? " AND deleted_at IS NULL" : "")
                + (lock ? " FOR UPDATE" : ""),
            (r, n) ->
                new Decision(
                    r.getObject("id", UUID.class),
                    r.getObject("review_id", UUID.class),
                    r.getString("status"),
                    r.getObject("base_version", Long.class),
                    time(r, "invalidated_at")),
            id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  private void admin(UserProfile actor) {
    if (actor == null
        || !actor.status().equals("active")
        || !Set.of("admin", "superadmin").contains(actor.role()))
      throw new BusinessException("ACCESS_DENIED", "需要管理员权限", HttpStatus.FORBIDDEN);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  public AuditDetail auditDetail(UserProfile actor, String type, UUID id) {
    admin(actor);
    if (!isType(type)) throw bad("审核类型不合法");
    boolean reply = type.endsWith("-reply");
    Decision item = decision(id, reply, false);
    if (item == null) throw missing();
    Identity root = identity(item.review(), false);
    if (!type.equals(type(root.target()) + (reply ? "-review-reply" : "-review"))) throw missing();
    Row submitted = reply ? reply(id, actor.id()) : submission(id, root.user());
    return new AuditDetail(
        true, root.current() == null ? null : submission(root.current(), root.user()), submitted);
  }

  @Transactional
  public ModerationResult moderate(
      UserProfile actor, String type, UUID id, ModerationRequest request) {
    admin(actor);
    if (!isType(type) || !Set.of("approve", "reject").contains(request.action()))
      throw bad("审核类型或操作不合法");
    String reason =
        request.reason() == null || request.reason().isBlank() ? null : text(request.reason());
    if (request.action().equals("reject") && reason == null) throw bad("请填写驳回原因");
    return decide(type, id, request.action(), reason, actor.id(), null);
  }

  private ModerationResult decide(
      String type, UUID id, String action, String reason, UUID actor, String model) {
    boolean reply = type.endsWith("-reply");
    Decision before = decision(id, reply, false);
    if (before == null) throw missing();
    Identity root = lockReview(before.review(), false);
    if (!type.equals(type(root.target()) + (reply ? "-review-reply" : "-review"))) throw missing();
    Decision row = decision(id, reply, true);
    if (row == null
        || !row.status().equals("pending")
        || row.invalidated() != null
        || root.deleted() != null
        || !reply && row.base() != root.version()) throw conflict("提交已审核或失效，请刷新");
    if (action.equals("approve")) {
      catalog(root.target(), false, true);
      if (reply && root.current() == null) throw missing();
    }
    db.update(
        "UPDATE "
            + (reply ? "foodtime.venue_review_replies" : "foodtime.venue_review_submissions")
            + " SET status=?,reviewed_by_user_id=?,reviewed_by_ai_model=?,reviewed_at=now(),review_reason=?"
            + " WHERE id=?",
        action.equals("approve") ? "approved" : "rejected",
        actor,
        model,
        reason,
        id);
    if (!reply && action.equals("approve"))
      db.update(
          "UPDATE foodtime.venue_reviews SET current_submission_id=? WHERE id=?", id, root.id());
    log(id, reply, actor, model, action, reason);
    return new ModerationResult(
        id,
        action.equals("approve") ? "approved" : "rejected",
        action.equals("approve") ? (reply ? id : root.id()) : null);
  }

  private void log(UUID id, boolean reply, UUID actor, String model, String action, String reason) {
    db.update(
        "INSERT INTO"
            + " foodtime.content_moderation_logs(id,venue_review_submission_id,venue_review_reply_id,moderator_user_id,actor,ai_model,action,reason)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        reply ? null : id,
        reply ? id : null,
        actor,
        model == null ? "human" : "ai",
        model,
        action,
        reason);
  }

  @Transactional(readOnly = true)
  public List<Pending> pending(int limit, UUID id) {
    return db.query(
        """
        SELECT q.* FROM (
        SELECT CASE WHEN t.stall_id IS NULL THEN 'hall-review' ELSE 'stall-review' END AS type,s.id,s.rating::integer AS rating,s.content,s.submitted_at AS created_at
        FROM foodtime.venue_review_submissions s JOIN foodtime.venue_reviews r ON r.id=s.review_id JOIN foodtime.venue_review_targets t ON t.id=r.target_id
        WHERE s.status='pending' AND s.invalidated_at IS NULL AND r.deleted_at IS NULL AND s.base_version=r.publication_version
        UNION ALL
        SELECT CASE WHEN t.stall_id IS NULL THEN 'hall-review-reply' ELSE 'stall-review-reply' END,c.id,0,c.content,c.created_at
        FROM foodtime.venue_review_replies c JOIN foodtime.venue_reviews r ON r.id=c.review_id JOIN foodtime.venue_review_targets t ON t.id=r.target_id
        WHERE c.status='pending' AND c.deleted_at IS NULL AND r.deleted_at IS NULL AND r.current_submission_id IS NOT NULL
        ) q WHERE NOT EXISTS(SELECT 1 FROM foodtime.ai_moderation_results a WHERE a.submission_type=q.type AND a.submission_id=q.id)
        """
            + (id == null ? "" : " AND q.id=?")
            + " ORDER BY created_at,id LIMIT ?",
        (r, n) ->
            new Pending(
                r.getString("type"),
                r.getObject("id", UUID.class),
                r.getInt("rating"),
                r.getString("content")),
        id == null ? new Object[] {limit} : new Object[] {id, limit});
  }

  @Transactional
  public int recordVerdict(Pending item, AiVerdict verdict) {
    if (!isType(item.type())) throw bad("审核类型不合法");
    boolean reply = item.type().endsWith("-reply");
    Decision before = decision(item.id(), reply, false);
    if (before == null) return 0;
    Identity root = lockReview(before.review(), false);
    Decision row = decision(item.id(), reply, true);
    if (row == null
        || !row.status().equals("pending")
        || row.invalidated() != null
        || root.deleted() != null
        || !reply && row.base() != root.version()) return 0;
    if (!item.type().equals(type(root.target()) + (reply ? "-review-reply" : "-review")))
      throw missing();
    int inserted =
        ai.insert(
            UUID.randomUUID(),
            item.type(),
            item.id(),
            verdict.riskLevel(),
            BigDecimal.valueOf(verdict.confidence()),
            verdict.labels().toArray(String[]::new),
            verdict.reason(),
            verdict.model());
    AiVerdict canonical =
        inserted == 1 ? verdict : ai.catalogVerdict(item.type(), item.id()).orElseThrow();
    boolean approve =
        canonical.riskLevel().equals("LOW") && canonical.confidence() >= policy.autoApproveAbove();
    boolean reject =
        canonical.riskLevel().equals("HIGH") && canonical.confidence() >= policy.autoRejectAbove();
    if ((approve || reject) && !"system".equals(canonical.model()))
      decide(
          item.type(),
          item.id(),
          approve ? "approve" : "reject",
          text(canonical.reason()),
          null,
          canonical.model());
    return inserted;
  }

  private String type(UUID target) {
    return db.queryForObject(
        "SELECT CASE WHEN stall_id IS NULL THEN 'hall' ELSE 'stall' END FROM"
            + " foodtime.venue_review_targets WHERE id=?",
        String.class,
        target);
  }

  private String text(String content) {
    if (content == null || content.strip().isBlank()) throw bad("请填写内容");
    String value = content.strip();
    if (value.codePointCount(0, value.length()) > 500) throw bad("内容不能超过500字");
    return value;
  }

  private BusinessException missing() {
    return new BusinessException("VENUE_REVIEW_NOT_FOUND", "评价不存在或暂不可用", HttpStatus.NOT_FOUND);
  }

  private BusinessException conflict(String message) {
    return new BusinessException("VENUE_REVIEW_CONFLICT", message, HttpStatus.CONFLICT);
  }

  private BusinessException bad(String message) {
    return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
  }
}
