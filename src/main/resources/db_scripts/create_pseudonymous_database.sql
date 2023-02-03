﻿-- Idea is to create a clone of as much the database as is needed to run. Switch the scheme over from public to anonymous
-- and run the regression tests (or test manually?) to check if it works. Maybe check for every database table that is
-- mentioned in the codebase.

CREATE OR REPLACE FUNCTION create_anonymous_database() RETURNS boolean
AS
$$
BEGIN

DROP SCHEMA IF EXISTS anonymous CASCADE;
CREATE SCHEMA anonymous;


-- User related tables:

CREATE TABLE anonymous.users AS
    SELECT
        id,
        _id,
        'FamilyName-' || id::varchar(255) as family_name,
        'GivenName-' || id::varchar(255) as given_name,
        id::varchar(255) || '@noreply.isaaccomputerscience.org' as email,
        role,
        NULL AS date_of_birth,
        'PREFER_NOT_TO_SAY' as gender,
        registration_date,
        school_id,
        NULL as school_other,
        registered_contexts,
        registered_contexts_last_confirmed,
        last_updated,
        email_verification_status,
        last_seen,
        NULL as email_to_verify,
        NULL as email_verification_token,
        0 as session_token,
        deleted
    FROM public.users;

-- linked_accounts ignored

CREATE TABLE anonymous.user_preferences AS SELECT * FROM public.user_preferences;

CREATE TABLE anonymous.user_associations_tokens AS SELECT * FROM public.user_associations_tokens;

-- external_accounts ignored

CREATE TABLE anonymous.user_badges AS SELECT * FROM public.user_badges;

CREATE TABLE anonymous.user_credentials AS
    SELECT
        user_id,
        'ihDEIMGwOldVWPsh4EFM/57OIpQIezcAVhP64KDrckPC7xwnGtiIvJW46fcDP9cov1I+5qOenqLlJwA3k+0zOg==' as password,
        'EZgUc/oK5iydm+VsHz8ZlA==' as secure_salt,
        'SegueSCryptv1' as security_scheme,
        NULL as reset_token,
        NULL as reset_expiry,
        created,
        last_updated
    FROM public.user_credentials;

-- user_totp ignored
--  It may be worth running the following post-database creation, with a valid secret key, so that admins can log in:
--   INSERT INTO user_totp SELECT id, '[ADD_SECRET_HERE]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE role='ADMIN';

-- user_email_preferences ignored

-- user_notifications ignored

-- user_alerts ignored

-- temporary_user_store ignored

-- ip_location_history ignored

CREATE TABLE anonymous.uk_post_codes AS SELECT * FROM public.uk_post_codes;


-- Group related tables:

CREATE TABLE anonymous.user_associations AS SELECT * FROM public.user_associations;

CREATE TABLE anonymous.groups AS
    SELECT
        id,
        'Group-' || id::varchar(255) as group_name,
        owner_id,
        created,
        archived,
        group_status,
        last_updated
    FROM public.groups;

CREATE TABLE anonymous.group_memberships AS SELECT * FROM public.group_memberships;

CREATE TABLE anonymous.group_additional_managers AS SELECT * FROM public.group_additional_managers;


-- Assignment related tables:

CREATE TABLE anonymous.gameboards AS
    SELECT
       id,
       'Gameboard-' || public.gameboards.id as title,
       contents,
       wildcard,
       wildcard_position,
       game_filter,
       owner_user_id,
       creation_method,
       creation_date,
       tags
    FROM public.gameboards;

CREATE TABLE anonymous.user_gameboards AS SELECT * FROM public.user_gameboards;

CREATE TABLE anonymous.assignments AS
    SELECT
        id,
        gameboard_id,
        group_id,
        owner_user_id,
        NULL as notes,
        creation_date,
        due_date,
        scheduled_start_date
    FROM public.assignments;


-- Event bookings:

CREATE TABLE anonymous.event_bookings AS
    SELECT
        id,
        event_id,
        created,
        user_id,
        reserved_by,
        status,
        updated,
        NULL as additional_booking_information,
        coalesce(pii_removed, CURRENT_TIMESTAMP) as pii_removed
    FROM public.event_bookings;


-- Question attempt related tables:

CREATE TABLE anonymous.question_attempts AS SELECT * FROM public.question_attempts;

CREATE TABLE anonymous.user_streak_freezes AS
    SELECT
        user_id,
        start_date,
        end_date,
        'Streak freeze' as comment
    FROM public.user_streak_freezes;

CREATE TABLE anonymous.user_streak_targets AS
    SELECT
        user_id,
        target_count,
        start_date,
        end_date,
        'Streak target' as comment
    FROM public.user_streak_targets;


-- Quiz tables:

CREATE TABLE anonymous.quiz_assignments AS SELECT * FROM public.quiz_assignments;

CREATE TABLE anonymous.quiz_attempts AS SELECT * FROM public.quiz_attempts;

CREATE TABLE anonymous.quiz_question_attempts AS SELECT * FROM public.quiz_question_attempts;


-- Logged events

CREATE TABLE anonymous.logged_events AS
    SELECT
        id,
        user_id,
        anonymous_user,
        event_type,
        json_build_object('placeholder', rpad('', greatest(0, length(event_details::TEXT) - 20), 'x')) as event_details,
        '192.168.1.1'::inet as ip_address,
        timestamp
    FROM public.logged_events;

-- Set sequences to correct values so that database can be used:
SELECT SETVAL('anonymous.assignments_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.assignments;
SELECT SETVAL('anonymous.event_bookings_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.event_bookings;
SELECT SETVAL('anonymous.groups_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.groups;
SELECT SETVAL('anonymous.ip_location_history_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.ip_location_history;
SELECT SETVAL('anonymous.logged_events_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.logged_events;
SELECT SETVAL('anonymous.question_attempts_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.question_attempts;
SELECT SETVAL('anonymous.quiz_assignments_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.quiz_assignments;
SELECT SETVAL('anonymous.quiz_attempts_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.quiz_attempts;
SELECT SETVAL('anonymous.quiz_question_attempts_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.quiz_question_attempts;
SELECT SETVAL('anonymous.users_id_seq', COALESCE(MAX(id), 1) ) FROM anonymous.users;


RETURN true;
END;
$$
LANGUAGE plpgsql;