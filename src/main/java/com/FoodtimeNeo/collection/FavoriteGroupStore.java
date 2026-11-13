package com.FoodtimeNeo.collection;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class FavoriteGroupStore {
    private final JdbcTemplate jdbc;

    public FavoriteGroupStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FavoriteGroupDtos.Response> list(UUID owner, FavoriteKind kind) {
        String members = memberTable(kind);
        return jdbc.query("""
                SELECT g.id, g.kind, g.name, g.sort_order, g.revision,
                       (SELECT count(*) FROM foodtime.%s m WHERE m.group_id = g.id) AS item_count
                FROM foodtime.favorite_groups g
                WHERE g.owner_id = ? AND g.kind = ?
                ORDER BY g.sort_order, g.created_at, g.id
                """.formatted(members), (row, index) -> new FavoriteGroupDtos.Response(
                row.getObject("id", UUID.class), row.getString("kind"), row.getString("name"),
                row.getInt("sort_order"), row.getLong("revision"), row.getLong("item_count")),
                owner, kind.wireName());
    }

    public FavoriteGroupDtos.Response find(UUID owner, UUID id) {
        return jdbc.query("""
                SELECT g.id, g.kind, g.name, g.sort_order, g.revision,
                       CASE g.kind
                           WHEN 'dish' THEN (SELECT count(*) FROM foodtime.dish_favorite_group_members m
                                             WHERE m.group_id = g.id)
                           ELSE (SELECT count(*) FROM foodtime.community_favorite_group_members m
                                 WHERE m.group_id = g.id)
                       END AS item_count
                FROM foodtime.favorite_groups g WHERE g.id = ? AND g.owner_id = ?
                """, (row, index) -> new FavoriteGroupDtos.Response(
                row.getObject("id", UUID.class), row.getString("kind"), row.getString("name"),
                row.getInt("sort_order"), row.getLong("revision"), row.getLong("item_count")),
                id, owner).stream().findFirst().orElse(null);
    }

    public String kindOf(UUID owner, UUID id) {
        return jdbc.query("SELECT kind FROM foodtime.favorite_groups WHERE id = ? AND owner_id = ?",
                (row, index) -> row.getString(1), id, owner).stream().findFirst().orElse("");
    }

    public void lockOwnerKind(UUID owner, FavoriteKind kind) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", row -> {},
                "favorite-groups:" + owner + ":" + kind.wireName());
    }

    public long count(UUID owner, FavoriteKind kind) {
        Long value = jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.favorite_groups WHERE owner_id = ? AND kind = ?",
                Long.class, owner, kind.wireName());
        return value == null ? 0 : value;
    }

    public boolean insert(UUID id, UUID owner, FavoriteKind kind, String name, int sortOrder) {
        return jdbc.update("""
                INSERT INTO foodtime.favorite_groups(id, owner_id, kind, name, sort_order)
                VALUES (?, ?, ?, ?, ?)
                """, id, owner, kind.wireName(), name, sortOrder) == 1;
    }

    public boolean update(UUID owner, UUID id, String name, Integer sortOrder, Long expectedRevision) {
        StringBuilder sql = new StringBuilder("""
                UPDATE foodtime.favorite_groups
                SET name = coalesce(?, name), sort_order = coalesce(?, sort_order),
                    revision = revision + 1, updated_at = now()
                WHERE id = ? AND owner_id = ?
                """);
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add(name);
        args.add(sortOrder);
        args.add(id);
        args.add(owner);
        if (expectedRevision != null) {
            sql.append(" AND revision = ?");
            args.add(expectedRevision);
        }
        return jdbc.update(sql.toString(), args.toArray()) == 1;
    }

    public boolean delete(UUID owner, UUID id) {
        return jdbc.update("DELETE FROM foodtime.favorite_groups WHERE id = ? AND owner_id = ?", id, owner) == 1;
    }

    public boolean favoriteExists(UUID owner, FavoriteKind kind, UUID targetId) {
        String table = kind == FavoriteKind.DISH ? "dish_favorites" : "community_favorites";
        String column = kind == FavoriteKind.DISH ? "dish_id" : "post_id";
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime." + table + " WHERE user_id = ? AND " + column + " = ?)",
                Boolean.class, owner, targetId));
    }

    public boolean addMember(UUID groupId, UUID owner, FavoriteKind kind, UUID targetId) {
        String members = memberTable(kind);
        String favorites = kind == FavoriteKind.DISH ? "dish_favorites" : "community_favorites";
        String column = kind == FavoriteKind.DISH ? "dish_id" : "post_id";
        return jdbc.update("""
                INSERT INTO foodtime.%s(group_id, owner_id, %s)
                SELECT ?, ?, ? WHERE EXISTS(
                    SELECT 1 FROM foodtime.%s WHERE user_id = ? AND %s = ?)
                ON CONFLICT DO NOTHING
                """.formatted(members, column, favorites, column),
                groupId, owner, targetId, owner, targetId) >= 0;
    }

    public void removeMember(UUID groupId, UUID owner, FavoriteKind kind, UUID targetId) {
        String members = memberTable(kind);
        String column = kind == FavoriteKind.DISH ? "dish_id" : "post_id";
        jdbc.update("DELETE FROM foodtime." + members + " WHERE group_id = ? AND owner_id = ? AND "
                + column + " = ?", groupId, owner, targetId);
    }

    public List<UUID> membershipIds(UUID owner, FavoriteKind kind, UUID targetId) {
        String members = memberTable(kind);
        String column = kind == FavoriteKind.DISH ? "dish_id" : "post_id";
        return jdbc.query("""
                SELECT m.group_id FROM foodtime.%s m
                JOIN foodtime.favorite_groups g ON g.id = m.group_id
                WHERE m.owner_id = ? AND m.%s = ? AND g.owner_id = ? AND g.kind = ?
                ORDER BY g.sort_order, g.created_at, g.id
                """.formatted(members, column),
                (row, index) -> row.getObject(1, UUID.class), owner, targetId, owner, kind.wireName());
    }

    private static String memberTable(FavoriteKind kind) {
        return kind == FavoriteKind.DISH ? "dish_favorite_group_members" : "community_favorite_group_members";
    }
}
