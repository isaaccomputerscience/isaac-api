-- Isaac Account Analytics Queries
-- Based on public.users table schema

-- 1. Total number of accounts on Isaac
SELECT COUNT(*) as total_accounts
FROM users
WHERE deleted = false;

-- 2. Number of student accounts
SELECT COUNT(*) as student_accounts
FROM users
WHERE role = 'STUDENT' AND deleted = false;

-- 3. Number of teacher accounts
SELECT COUNT(*) as teacher_accounts
FROM users
WHERE role = 'TEACHER' AND deleted = false;

-- 4. Average number of active accounts per month
-- Monthly breakdown of active users (by last_seen)
SELECT
    TO_CHAR(last_seen, 'YYYY-MM') as month,
    COUNT(DISTINCT id) as active_accounts
FROM users
WHERE last_seen IS NOT NULL AND deleted = false
GROUP BY TO_CHAR(last_seen, 'YYYY-MM')
ORDER BY month DESC;

-- OR just the average across all months:
SELECT
    ROUND(AVG(monthly_active)::numeric, 2) as avg_active_per_month
FROM (
    SELECT
        TO_CHAR(last_seen, 'YYYY-MM') as month,
        COUNT(DISTINCT id) as monthly_active
    FROM users
    WHERE last_seen IS NOT NULL AND deleted = false
    GROUP BY TO_CHAR(last_seen, 'YYYY-MM')
) monthly_counts;

-- 5. New student accounts created per month (average)
SELECT
    ROUND(AVG(monthly_new)::numeric, 2) as avg_new_students_per_month
FROM (
    SELECT
        TO_CHAR(registration_date, 'YYYY-MM') as month,
        COUNT(*) as monthly_new
    FROM users
    WHERE role = 'STUDENT' AND registration_date IS NOT NULL AND deleted = false
    GROUP BY TO_CHAR(registration_date, 'YYYY-MM')
) monthly_counts;

-- Monthly breakdown of new student accounts
SELECT
    TO_CHAR(registration_date, 'YYYY-MM') as month,
    COUNT(*) as new_students
FROM users
WHERE role = 'STUDENT' AND registration_date IS NOT NULL AND deleted = false
GROUP BY TO_CHAR(registration_date, 'YYYY-MM')
ORDER BY month DESC;

-- 6. New teacher accounts created per month (average)
SELECT
    ROUND(AVG(monthly_new)::numeric, 2) as avg_new_teachers_per_month
FROM (
    SELECT
        TO_CHAR(registration_date, 'YYYY-MM') as month,
        COUNT(*) as monthly_new
    FROM users
    WHERE role = 'TEACHER' AND registration_date IS NOT NULL AND deleted = false
    GROUP BY TO_CHAR(registration_date, 'YYYY-MM')
) monthly_counts;

-- Monthly breakdown of new teacher accounts
SELECT
    TO_CHAR(registration_date, 'YYYY-MM') as month,
    COUNT(*) as new_teachers
FROM users
WHERE role = 'TEACHER' AND registration_date IS NOT NULL AND deleted = false
GROUP BY TO_CHAR(registration_date, 'YYYY-MM')
ORDER BY month DESC;

-- BONUS: Comprehensive monthly breakdown
SELECT
    TO_CHAR(registration_date, 'YYYY-MM') as month,
    role,
    COUNT(*) as new_accounts
FROM users
WHERE registration_date IS NOT NULL AND deleted = false
GROUP BY TO_CHAR(registration_date, 'YYYY-MM'), role
ORDER BY month DESC, role;