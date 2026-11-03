-- Local evaluation data only. Adds a new isolated dataset on every run; never deletes existing data.

-- Run against the local vaccination_portal database after Flyway V26.

-- All demo accounts use VivaDemo!2026. These are synthetic accounts with .test email addresses.

-- Seeded eligibility is preparation data, not evidence that a real clinical review was performed.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET time_zone = '+00:00';

START TRANSACTION;

SET @now = UTC_TIMESTAMP(6);

SET @run = DATE_FORMAT(@now, '%Y%m%d%H%i%s');

SET @code = CONCAT('MF03-VIVA-', @run);

SET @hash = '$2a$10$u20rDSOx6Xgd56WJoIwebeGNJej.A4VBhPI4a8hq03wg.6tUblghe';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo staff',CONCAT('mf03.staff.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @staff = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @staff,id FROM roles WHERE code='ROLE_CENTER_MANAGER';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo reviewer',CONCAT('mf03.reviewer.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @reviewer = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @reviewer,id FROM roles WHERE code='ROLE_HEALTHCARE_OFFICER';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo book',CONCAT('mf03.book.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @book = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @book,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo manage',CONCAT('mf03.manage.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @manage = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @manage,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo holder',CONCAT('mf03.holder.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @holder = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @holder,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo join',CONCAT('mf03.join.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @join = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @join,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo waiter',CONCAT('mf03.waiter.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @waiter = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @waiter,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo leave',CONCAT('mf03.leave.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @leave = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @leave,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo checkin',CONCAT('mf03.checkin.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @checkin = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @checkin,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo reverse',CONCAT('mf03.reverse.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @reverse = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @reverse,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo noshow',CONCAT('mf03.noshow.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @noshow = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @noshow,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo history',CONCAT('mf03.history.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @history = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @history,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo ineligible',CONCAT('mf03.ineligible.',@run,'@example.test'),@hash,'ACTIVE',@now);

SET @ineligible = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @ineligible,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO user_accounts(full_name,email,password_hash,account_status,email_verified_at) VALUES ('MF03 Demo unverified',CONCAT('mf03.unverified.',@run,'@example.test'),@hash,'ACTIVE',NULL);

SET @unverified = LAST_INSERT_ID();

INSERT INTO user_roles(user_id,role_id) SELECT @unverified,id FROM roles WHERE code='ROLE_CITIZEN';

INSERT INTO vaccine_types(code,name,dose_unit,active) VALUES (@code,'MF03 Synthetic Demo Vaccine','dose',TRUE);

SET @vaccine=LAST_INSERT_ID();

INSERT INTO target_groups(code,description,active) VALUES (@code,'MF03 Synthetic Demo Adults',TRUE);

SET @group=LAST_INSERT_ID();

INSERT INTO vaccination_centers(code,name,address,contact,timezone,center_status,safe_concurrent_occupancy,session_service_ceiling) VALUES (@code,'MF03 Viva Demo Center','Synthetic demo location, Colombo','0110000000','Asia/Colombo','ACTIVE',100,200);

SET @center=LAST_INSERT_ID();

INSERT INTO operational_staff_rosters(center_id,user_id,role,status) VALUES (@center,@staff,'SUPERVISOR','ACTIVE');

INSERT INTO vaccination_campaigns(code,name,description,vaccine_type_id,target_group_id,service_start_date,service_end_date,booking_open_at,booking_close_at,status) VALUES (@code,'MF03 Viva Demo Campaign','Synthetic appointments and waitlists for local evaluation',@vaccine,@group,DATE(@now)-INTERVAL 1 DAY,DATE(@now)+INTERVAL 30 DAY,@now-INTERVAL 1 DAY,@now+INTERVAL 29 DAY,'ACTIVE');

SET @campaign=LAST_INSERT_ID();

INSERT INTO campaign_center_assignments(campaign_id,center_id,active_from,active_to,assignment_status) VALUES (@campaign,@center,DATE(@now)-INTERVAL 1 DAY,DATE(@now)+INTERVAL 30 DAY,'ACTIVE');

INSERT INTO campaign_dose_programmes(campaign_id,dose_category,permitted_dose_numbers,active_from) VALUES (@campaign,'PRIMARY','1',DATE(@now)-INTERVAL 1 DAY);

INSERT INTO questionnaire(code,name) VALUES (@code,'MF03 Demo Screening');

SET @questionnaire=LAST_INSERT_ID();

INSERT INTO questionnaire_version(questionnaire_id,version_number,status,published_at) VALUES (@questionnaire,1,'PUBLISHED',@now);

SET @version=LAST_INSERT_ID();

INSERT INTO screening_question(code,text,question_type) VALUES (@code,'Synthetic demo: any contraindications?','BOOLEAN');

SET @question=LAST_INSERT_ID();

INSERT INTO questionnaire_item(version_id,question_id,display_order) VALUES (@version,@question,1);

INSERT INTO campaign_questionnaire_assignments(campaign_id,questionnaire_version_id,active_from,active_to) VALUES (@campaign,@version,DATE(@now)-INTERVAL 1 DAY,DATE(@now)+INTERVAL 30 DAY);

INSERT INTO consent_notice_version(notice_code,version_number,title,content,content_hash,published_at) VALUES (@code,1,'Demo consent','Synthetic local evaluation data only',SHA2('Synthetic local evaluation data only',256),@now);

SET @consent=LAST_INSERT_ID();

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@book,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@book,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@manage,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@manage,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@holder,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@holder,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@join,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@join,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@waiter,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@waiter,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@leave,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@leave,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@checkin,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@checkin,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@reverse,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@reverse,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@noshow,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@noshow,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@history,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@history,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@ineligible,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@ineligible,@campaign,@reviewer,'INELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

INSERT INTO screening_submission(citizen_id,campaign_id,version_id,consent_notice_version_id,status,preliminary_result,consent_at,submitted_at) VALUES (@unverified,@campaign,@version,@consent,'REVIEWED','PRELIMINARY_REVIEW_REQUIRED',@now,@now);

SET @submission=LAST_INSERT_ID();

INSERT INTO screening_answer(submission_id,question_id,typed_answer) VALUES (@submission,@question,'false');

INSERT INTO eligibility_assessment(submission_id,citizen_id,campaign_id,reviewer_id,status,preliminary_result,reason,reviewed_at,valid_until) VALUES (@submission,@unverified,@campaign,@reviewer,'ELIGIBLE','PRELIMINARY_REVIEW_REQUIRED','SYNTHETIC DEMO SEED - not a real clinical assessment',@now,@now+INTERVAL 30 DAY);

SET @start=@now+INTERVAL 48 HOUR;

INSERT INTO vaccination_sessions(campaign_id,center_id,start_at,end_at,booking_close_at,schedule_timezone,lifecycle_status) VALUES (@campaign,@center,@start,@start+INTERVAL 2 HOUR,@start-INTERVAL 30 MINUTE,'Asia/Colombo','SCHEDULED');

SET @futuresession=LAST_INSERT_ID();

INSERT INTO time_slots(session_id,start_time,end_time,capacity,reserved_count) VALUES (@futuresession,@start,@start+INTERVAL 30 MINUTE,10,0);

SET @futureslot=LAST_INSERT_ID();

INSERT INTO time_slots(session_id,start_time,end_time,capacity,reserved_count) VALUES (@futuresession,@start+INTERVAL 1 HOUR,@start+INTERVAL 90 MINUTE,10,0);

SET @replacementslot=LAST_INSERT_ID();

SET @start=@now+INTERVAL 96 HOUR;

INSERT INTO vaccination_sessions(campaign_id,center_id,start_at,end_at,booking_close_at,schedule_timezone,lifecycle_status) VALUES (@campaign,@center,@start,@start+INTERVAL 2 HOUR,@start-INTERVAL 30 MINUTE,'Asia/Colombo','SCHEDULED');

SET @fullsession=LAST_INSERT_ID();

INSERT INTO time_slots(session_id,start_time,end_time,capacity,reserved_count) VALUES (@fullsession,@start,@start+INTERVAL 30 MINUTE,1,0);

SET @fullslot=LAST_INSERT_ID();

SET @start=@now+INTERVAL 10 MINUTE;

INSERT INTO vaccination_sessions(campaign_id,center_id,start_at,end_at,booking_close_at,schedule_timezone,lifecycle_status) VALUES (@campaign,@center,@start,@start+INTERVAL 2 HOUR,@start-INTERVAL 30 MINUTE,'Asia/Colombo','SCHEDULED');

SET @arrivalsession=LAST_INSERT_ID();

INSERT INTO time_slots(session_id,start_time,end_time,capacity,reserved_count) VALUES (@arrivalsession,@start,@start+INTERVAL 30 MINUTE,10,0);

SET @arrivalslot=LAST_INSERT_ID();

SET @start=@now-INTERVAL 60 MINUTE;

INSERT INTO vaccination_sessions(campaign_id,center_id,start_at,end_at,booking_close_at,schedule_timezone,lifecycle_status) VALUES (@campaign,@center,@start,@start+INTERVAL 2 HOUR,@start-INTERVAL 30 MINUTE,'Asia/Colombo','SCHEDULED');

SET @pastsession=LAST_INSERT_ID();

INSERT INTO time_slots(session_id,start_time,end_time,capacity,reserved_count) VALUES (@pastsession,@start,@start+INTERVAL 30 MINUTE,10,0);

SET @pastslot=LAST_INSERT_ID();

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@manage,@campaign,@futureslot,'CONFIRMED',@now-INTERVAL 1 DAY,NULL);

SET @manageappointment=LAST_INSERT_ID();

UPDATE time_slots SET reserved_count=reserved_count+1 WHERE id=@futureslot;

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@holder,@campaign,@fullslot,'CONFIRMED',@now-INTERVAL 1 DAY,NULL);

SET @holderappointment=LAST_INSERT_ID();

UPDATE time_slots SET reserved_count=reserved_count+1 WHERE id=@fullslot;

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@checkin,@campaign,@arrivalslot,'CONFIRMED',@now-INTERVAL 1 DAY,NULL);

SET @checkinappointment=LAST_INSERT_ID();

UPDATE time_slots SET reserved_count=reserved_count+1 WHERE id=@arrivalslot;

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@reverse,@campaign,@arrivalslot,'CHECKED_IN',@now-INTERVAL 1 DAY,NULL);

SET @reverseappointment=LAST_INSERT_ID();

UPDATE time_slots SET reserved_count=reserved_count+1 WHERE id=@arrivalslot;

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@noshow,@campaign,@pastslot,'CONFIRMED',@now-INTERVAL 1 DAY,NULL);

SET @noshowappointment=LAST_INSERT_ID();

UPDATE time_slots SET reserved_count=reserved_count+1 WHERE id=@pastslot;

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@history,@campaign,@pastslot,'CANCELLED',@now-INTERVAL 1 DAY,@now-INTERVAL 2 HOUR);

SET @historyappointment=LAST_INSERT_ID();

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@history,@campaign,@pastslot,'NO_SHOW',@now-INTERVAL 1 DAY,NULL);

SET @historyappointment=LAST_INSERT_ID();

INSERT INTO appointment(citizen_id,campaign_id,slot_id,status,booked_at,cancelled_at) VALUES (@history,@campaign,@pastslot,'COMPLETED',@now-INTERVAL 1 DAY,NULL);

SET @historyappointment=LAST_INSERT_ID();

UPDATE time_slots SET reserved_count=reserved_count+1 WHERE id=@pastslot;

INSERT INTO waitlist_entry(citizen_id,campaign_id,session_id,status,joined_at) VALUES (@waiter,@campaign,@fullsession,'WAITING',@now-INTERVAL 20 MINUTE);

SET @waiterentry=LAST_INSERT_ID();

INSERT INTO waitlist_entry(citizen_id,campaign_id,session_id,status,joined_at) VALUES (@leave,@campaign,@fullsession,'WAITING',@now-INTERVAL 10 MINUTE);

SET @leaveentry=LAST_INSERT_ID();

-- Screening UI looks up this canonical notice code, regardless of the campaign.
INSERT INTO consent_notice_version(notice_code,version_number,title,content,content_hash,published_at)
SELECT 'PRE_VACCINATION_SCREENING',COALESCE(MAX(version_number),0)+1,'Local demo screening consent','LOCAL DEMONSTRATION ONLY. I understand this screening form uses synthetic evaluation data. I agree to submit my demo answers for eligibility review. This notice is for local evaluation and is not a production consent policy.',SHA2('LOCAL DEMONSTRATION ONLY. I understand this screening form uses synthetic evaluation data. I agree to submit my demo answers for eligibility review. This notice is for local evaluation and is not a production consent policy.',256),@now
FROM consent_notice_version WHERE notice_code='PRE_VACCINATION_SCREENING'
HAVING SUM(CASE WHEN retired_at IS NULL THEN 1 ELSE 0 END)=0 OR COUNT(*)=0;

COMMIT;

SELECT @code AS campaign_code,@campaign AS campaign_id,@center AS center_id,@futuresession AS future_session_id,@fullsession AS full_session_id,@futureslot AS book_slot_id,@replacementslot AS replacement_slot_id,@fullslot AS full_slot_id,@arrivalslot AS checkin_slot_id,@pastslot AS no_show_slot_id;

SELECT id,full_name,email FROM user_accounts WHERE email LIKE CONCAT('mf03.%.',@run,'@example.test') ORDER BY id;

SELECT id,citizen_id,slot_id,status FROM appointment WHERE campaign_id=@campaign ORDER BY id;

SELECT id,citizen_id,session_id,status FROM waitlist_entry WHERE campaign_id=@campaign ORDER BY joined_at,id;
