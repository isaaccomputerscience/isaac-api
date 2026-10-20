-- =============================================================
-- TEACHER ID LIST (approved teachers only, excludes teacher_pending)
-- Outputs a single row: a comma-separated list of user IDs.
-- =============================================================
SELECT string_agg(id::text, ',' ORDER BY id) AS teacher_ids
FROM (
    SELECT DISTINCT u.id
    FROM users u
    WHERE u.deleted = false
      AND u.last_seen >= CURRENT_DATE - INTERVAL '2 years'
      AND u.email_verification_status IN ('VERIFIED', 'NOT_VERIFIED')
      AND u.role = 'TEACHER'
      AND (u.teacher_pending = false OR u.teacher_pending IS NULL)
      AND EXISTS (
            SELECT 1
            FROM unnest(u.registered_contexts) AS ctx
            WHERE ctx ->> 'stage' IN ('gcse', 'a_level', 'all')
          )
      AND EXISTS (
            SELECT 1
            FROM user_preferences up
            WHERE up.user_id = u.id
              AND up.preference_type = 'EMAIL_PREFERENCE'
              AND up.preference_name = 'NEWS_AND_UPDATES'
              AND up.preference_value = true
          )
) sub;

-- To write straight to a .txt file via psql:
-- psql "<connection string>" -t -A -f event_email_teacher_ids.sql -o teacher_ids.txt
