package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.vaccination.dto.VaccinationApiErrorResponse;
import com.se2030.vaccinationportal.vaccination.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.exception.CertificateVerificationNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DigitalCertificateNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateDigitalCertificateException;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidCertificateVerificationTokenException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Module-scoped HTTP exception translation for MF-05 vaccination endpoints.
 */
@RestControllerAdvice(basePackages = "com.se2030.vaccinationportal.vaccination.controller")
public class VaccinationExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, ex.getMessage() != null ? ex.getMessage() : "Access is denied", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<VaccinationApiErrorResponse.FieldErrorItem> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                "Validation failed for request payload",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "Malformed JSON request body or invalid field type", request, null);
    }

    @ExceptionHandler(VaccinationDraftNotFoundException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleNotFound(
            VaccinationDraftNotFoundException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(VaccinationAppointmentNotFoundException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleAppointmentNotFound(
            VaccinationAppointmentNotFoundException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateVaccinationDraftException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleDuplicate(
            DuplicateVaccinationDraftException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AppointmentNotCheckedInException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleAppointmentNotCheckedIn(
            AppointmentNotCheckedInException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(VaccinationRecordAlreadyExistsException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleRecordAlreadyExists(
            VaccinationRecordAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidVaccinationDraftException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleInvalidDraft(
            InvalidVaccinationDraftException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DigitalCertificateNotFoundException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleCertificateNotFound(
            DigitalCertificateNotFoundException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(VaccinationRecordNotFoundException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleVaccinationRecordNotFound(
            VaccinationRecordNotFoundException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateDigitalCertificateException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleDuplicateCertificate(
            DuplicateDigitalCertificateException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(CertificateVerificationNotFoundException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleVerificationNotFound(
            CertificateVerificationNotFoundException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidCertificateVerificationTokenException.class)
    public ResponseEntity<VaccinationApiErrorResponse> handleInvalidVerificationToken(
            InvalidCertificateVerificationTokenException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    private ResponseEntity<VaccinationApiErrorResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<VaccinationApiErrorResponse.FieldErrorItem> fieldErrors
    ) {
        VaccinationApiErrorResponse body = new VaccinationApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    private VaccinationApiErrorResponse.FieldErrorItem mapFieldError(FieldError fieldError) {
        return new VaccinationApiErrorResponse.FieldErrorItem(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }
}
