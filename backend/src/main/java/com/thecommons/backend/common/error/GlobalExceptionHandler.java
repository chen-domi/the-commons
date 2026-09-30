package com.thecommons.backend.common.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thecommons.backend.inventory.exception.DuplicateQrCodeException;
import com.thecommons.backend.borrowing.BorrowingRequestNotFoundException;
import com.thecommons.backend.borrowing.InsufficientInventoryException;
import com.thecommons.backend.borrowing.InvalidBorrowingRequestException;
import com.thecommons.backend.borrowing.InvalidBorrowingRequestStateException;
import com.thecommons.backend.inventory.exception.InventoryItemAlreadyCheckedOutException;
import com.thecommons.backend.inventory.exception.InventoryItemNotCheckedOutException;
import com.thecommons.backend.inventory.exception.InventoryItemNotFoundException;
import com.thecommons.backend.organization.InvalidOrganizationJoinCodeException;
import com.thecommons.backend.organization.OrganizationAlreadyExistsException;
import com.thecommons.backend.organization.OrganizationNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(BorrowingRequestNotFoundException.class)
        public ResponseEntity<ApiError> handleBorrowingRequestNotFound(
                        BorrowingRequestNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "BORROWING_REQUEST_NOT_FOUND",
                                exception.getMessage()));
        }

        @ExceptionHandler(InvalidBorrowingRequestException.class)
        public ResponseEntity<ApiError> handleInvalidBorrowingRequest(
                        InvalidBorrowingRequestException exception) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                                Instant.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_BORROWING_REQUEST",
                                exception.getMessage()));
        }

        @ExceptionHandler({
                InvalidBorrowingRequestStateException.class,
                InsufficientInventoryException.class
        })
        public ResponseEntity<ApiError> handleBorrowingConflict(
                        RuntimeException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                "BORROWING_REQUEST_CONFLICT",
                                exception.getMessage()));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(
                        AccessDeniedException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.FORBIDDEN.value(),
                                "ACCESS_DENIED",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(error);
        }

        @ExceptionHandler(InvalidOrganizationJoinCodeException.class)
        public ResponseEntity<ApiError> handleInvalidOrganizationJoinCode(
                        InvalidOrganizationJoinCodeException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_ORGANIZATION_JOIN_CODE",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(error);
        }

        @ExceptionHandler(OrganizationAlreadyExistsException.class)
        public ResponseEntity<ApiError> handleOrganizationAlreadyExists(
                        OrganizationAlreadyExistsException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                "ORGANIZATION_ALREADY_EXISTS",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(error);
        }

        @ExceptionHandler(OrganizationNotFoundException.class)
        public ResponseEntity<ApiError> handleOrganizationNotFound(
                        OrganizationNotFoundException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "ORGANIZATION_NOT_FOUND",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(DuplicateQrCodeException.class)
        public ResponseEntity<ApiError> handleDuplicateQrCode(
                        DuplicateQrCodeException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                "DUPLICATE_QR_CODE",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(
                        MethodArgumentNotValidException exception) {

                StringBuilder message = new StringBuilder();

                for (FieldError error : exception.getBindingResult().getFieldErrors()) {
                        if (!message.isEmpty()) {
                                message.append("; ");
                        }

                        message.append(error.getField())
                                        .append(": ")
                                        .append(error.getDefaultMessage());
                }

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_FAILED",
                                message.toString());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(error);
        }

        @ExceptionHandler(InventoryItemNotFoundException.class)
        public ResponseEntity<ApiError> handleInventoryItemNotFound(
                        InventoryItemNotFoundException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "INVENTORY_ITEM_NOT_FOUND",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(InventoryItemAlreadyCheckedOutException.class)
        public ResponseEntity<ApiError> handleAlreadyCheckedOut(
                        InventoryItemAlreadyCheckedOutException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                "INVENTORY_ITEM_ALREADY_CHECKED_OUT",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(error);
        }

        @ExceptionHandler(InventoryItemNotCheckedOutException.class)
        public ResponseEntity<ApiError> handleNotCheckedOut(
                        InventoryItemNotCheckedOutException exception) {

                ApiError error = new ApiError(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                "INVENTORY_ITEM_NOT_CHECKED_OUT",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(error);
        }

}
