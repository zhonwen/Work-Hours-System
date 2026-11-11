package com.gsms.gsms.infra.config;

import com.gsms.gsms.infra.exception.CommonErrorCode;
import com.gsms.gsms.infra.common.Result;
import com.gsms.gsms.infra.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 按优先级顺序处理：业务异常 > 参数校验 > 常见运行时异常 > 数据库异常 > 通用异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(HttpServletRequest request, BusinessException e) {
        logger.warn("请求地址：{}，业务异常：[code={}] {}", request.getRequestURL(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(HttpServletRequest request, MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("请求地址：{}，参数校验失败：{}", request.getRequestURL(), errorMsg);
        return Result.error(CommonErrorCode.PARAM_ERROR.getCode(), "参数校验失败：" + errorMsg);
    }
    
    /**
     * 处理绑定异常（表单提交）
     */
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(HttpServletRequest request, BindException e) {
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("请求地址：{}，参数绑定失败：{}", request.getRequestURL(), errorMsg);
        return Result.error(CommonErrorCode.PARAM_ERROR.getCode(), "参数错误：" + errorMsg);
    }

    /**
     * 路径参数类型不匹配（如 /users/preference 误匹配 /{id}）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleTypeMismatch(HttpServletRequest request, MethodArgumentTypeMismatchException e) {
        logger.warn("请求地址：{}，参数类型错误：{}", request.getRequestURL(), e.getMessage());
        return Result.error(CommonErrorCode.PARAM_TYPE_ERROR.getCode(), "参数类型错误");
    }

    /**
     * 处理所有未捕获异常（兜底）：业务码统一 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleException(HttpServletRequest request, Exception e) {
        logger.error("请求地址：{}，系统异常：{}", request.getRequestURL(), e.getMessage(), e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        logger.warn("请求地址：{}，非法参数：{}", request.getRequestURL(), e.getMessage());
        return Result.error(CommonErrorCode.PARAM_INVALID.getCode(), e.getMessage());
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleNullPointerException(HttpServletRequest request, NullPointerException e) {
        logger.error("请求地址：{}，空指针异常：", request.getRequestURL(), e);
        return Result.error(500, "系统内部错误");
    }
    
    /**
     * 处理数据访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleDataAccessException(HttpServletRequest request, DataAccessException e) {
        logger.error("请求地址：{}，数据访问异常：{}", request.getRequestURL(), e.getMessage(), e);
        return Result.error(500, "数据访问异常，请稍后再试");
    }
}