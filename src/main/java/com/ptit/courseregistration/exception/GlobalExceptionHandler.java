package com.ptit.courseregistration.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Chuyen loi nghiep vu thanh flash message (DOAN.md muc 12).
 *
 * CO Y KHONG bat Exception.class: mot handler bat tat ca se nuot luon
 * AccessDeniedException cua Spring Security va bien loi 403 thanh 500.
 * Loi ngoai du kien de Spring Boot xu ly, hien qua templates/error.html.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Khong tim thay: hien trang 404 chu khong quay lai trang truoc. */
    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(NotFoundException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("message", ex.getMessage());
        // View error/404 nam trong templates/error/404.html
        return "error/404";
    }

    /**
     * Moi loi nghiep vu con lai: giu nguoi dung o lai trang dang lam,
     * kem thong bao do. Log muc info vi day la luong binh thuong cua ung dung
     * (lop day, trung lich...), khong phai su co.
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex,
                                 HttpServletRequest request,
                                 RedirectAttributes flash) {
        log.info("Loi nghiep vu [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage());
        flash.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + safeBackTarget(request);
    }

    /**
     * Rang buoc tang DB bi vi pham (muc 7.6). Truong hop hay gap nhat la
     * UNIQUE(student_id, section_id) chan bam nut hai lan / F5 / mang lag --
     * race condition ma row lock tren class_sections khong chan duoc.
     *
     * Khong lo message goc cua DB ra man hinh: no chua ten bang, ten index.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes flash) {
        log.warn("Vi pham rang buoc tang DB: {}", ex.getMostSpecificCause().getMessage());
        flash.addFlashAttribute("errorMessage",
                "Yêu cầu bị trùng hoặc vi phạm ràng buộc dữ liệu. Vui lòng tải lại trang và thử lại.");
        return "redirect:" + safeBackTarget(request);
    }

    /**
     * Lay duong dan de quay lai tu header Referer.
     *
     * Referer do trinh duyet gui nen la du lieu KHONG dang tin. Neu redirect thang
     * theo no thi thanh lo hong open redirect: ke tan cong dua nguoi dung toi mot
     * trang gia mao. Vi vay chi nhan phan duong dan tuong doi va chi khi cung host,
     * moi truong hop khac tra ve trang goc.
     */
    private String safeBackTarget(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = new URI(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return "/";
            }
            String path = uri.getRawPath();
            // path bat dau bang "//" se bi trinh duyet hieu la protocol-relative URL
            if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
                return "/";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (URISyntaxException e) {
            return "/";
        }
    }
}
