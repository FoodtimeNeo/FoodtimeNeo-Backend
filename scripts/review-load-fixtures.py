"""Generate, but never execute, SQL for a disposable completion_lab database (V18)."""
import argparse
import json
import re
import uuid
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database', required=True)
    parser.add_argument('--output-directory', type=Path, required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'[a-z][a-z0-9_]*completion_lab', args.database):
        parser.error('Use a new disposable database ending in completion_lab')
    dish, root = str(uuid.uuid4()), str(uuid.uuid4())
    users = [str(uuid.uuid4()) for _ in range(150)]
    emails = [f'202697{i:06}@bjtu.edu.cn' for i in range(150)]
    stall = '00000000-0000-4000-8000-000000000201'
    source = '00000000-0000-4000-8000-000000000001'
    moderator = '00000000-0000-4000-8000-000000000002'
    sql = ['BEGIN;', f"DO $$ BEGIN IF current_database() <> '{args.database}' THEN RAISE EXCEPTION 'isolated completion database required'; END IF; "
           f"IF NOT EXISTS(SELECT 1 FROM foodtime.users WHERE id='{source}') THEN RAISE EXCEPTION 'local integration fixtures required'; END IF; END $$;"]
    for user, email in zip(users, emails):
        sql.append(f"INSERT INTO foodtime.users(id,email,password_hash,display_name,email_verified_at) SELECT '{user}','{email}',password_hash,'load fixture',now() FROM foodtime.users WHERE id='{source}';")
    # UUIDs and background email names are unique per generation; no existing data is replaced.
    namespace = str(uuid.uuid4())
    sql += [f"INSERT INTO foodtime.users(id,email,password_hash,display_name,email_verified_at) SELECT md5('{namespace}-user-'||n)::uuid,'load-{namespace}-'||n||'@bjtu.edu.cn',u.password_hash,'background fixture',now() FROM generate_series(1,5000) n CROSS JOIN foodtime.users u WHERE u.id='{source}';",
            f"INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES ('{dish}','{stall}','isolated load fixture',10);",
            f"INSERT INTO foodtime.dish_reviews(id,dish_id,user_id,rating,content) VALUES ('{root}','{dish}','{users[0]}',4,'hot review');",
            f"INSERT INTO foodtime.dish_reviews(id,dish_id,user_id,rating,content) SELECT md5('{namespace}-review-'||n)::uuid,'{dish}',md5('{namespace}-user-'||n)::uuid,4,'background review' FROM generate_series(1,5000) n;"]
    replies = [str(uuid.uuid4()) for _ in range(100)]
    for index, reply in enumerate(replies):
        sql += [f"INSERT INTO foodtime.dish_review_replies(id,review_id,user_id,client_request_id,content) VALUES ('{reply}','{root}','{users[1]}','{uuid.uuid4()}','load reply {index}');",
                f"UPDATE foodtime.dish_review_replies SET status='approved',reviewed_by_user_id='{moderator}',reviewed_at=now() WHERE id='{reply}';"]
    sql += ['COMMIT;', 'ANALYZE foodtime.dish_reviews;', 'ANALYZE foodtime.dish_review_stats;', 'ANALYZE foodtime.dish_review_replies;']
    args.output_directory.mkdir(parents=True, exist_ok=True)
    (args.output_directory / 'seed.sql').write_text('\n'.join(sql) + '\n', encoding='utf-8')
    (args.output_directory / 'fixtures.json').write_text(json.dumps({'dish': dish, 'review': root, 'reply': replies[0],
        'review_count': 5001, 'reply_count': 100, 'emails': emails}, indent=2) + '\n', encoding='utf-8')
    print('Generated seed.sql and fixtures.json; inspect SQL before applying to the new disposable database.')


if __name__ == '__main__':
    main()
