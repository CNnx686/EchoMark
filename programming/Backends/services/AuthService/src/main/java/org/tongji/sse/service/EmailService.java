package org.tongji.sse.service;

/**
 * EmailService 接口定义了发送电子邮件的功能。
 */
public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}
