package com.se2030.vaccinationportal.appointment.web;

import com.se2030.vaccinationportal.appointment.dto.ErrorResponse;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.exception.DuplicateActiveAppointmentException;
import com.se2030.vaccinationportal.appointment.exception.DuplicateWaitlistEntryException;
import com.se2030.vaccinationportal.appointment.exception.InvalidAppointmentStateException;
import com.se2030.vaccinationportal.appointment.exception.InvalidCheckInWindowException;
import com.se2030.vaccinationportal.appointment.exception.NotEligibleException;
import com.se2030.vaccinationportal.appointment.exception.RequiresStaffOverrideException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice translating domain exceptions thrown during appointment and waitlist operations to standard HTTP status responses.
 */
@RestControllerAdvice(basePackages = "com.se2030.vaccinationportal.appointment")
public class AppointmentExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(NotEligibleException.class)
    public ResponseEntity<ErrorResponse> handleNotEligible(NotEligibleException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(RequiresStaffOverrideException.class)
    public ResponseEntity<ErrorResponse> handleRequiresStaffOverride(RequiresStaffOverrideException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateActiveAppointmentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateActiveAppointment(DuplicateActiveAppointmentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateWaitlistEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateWaitlistEntry(DuplicateWaitlistEntryException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(SlotFullException.class)
    public ResponseEntity<ErrorResponse> handleSlotFull(SlotFullException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(AppointmentAlreadyCompletedException.class)
    public ResponseEntity<ErrorResponse> handleAppointmentAlreadyCompleted(AppointmentAlreadyCompletedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCheckInWindowException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCheckInWindow(InvalidCheckInWindowException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(InvalidAppointmentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAppointmentState(InvalidAppointmentStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAppointmentNotFound(AppointmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgumentOrState(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Validation failure"
                : ex.getBindingResult().getFieldErrors().get(0).getField() + ": "
                + ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", msg));
    }
}
