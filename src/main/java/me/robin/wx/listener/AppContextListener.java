package me.robin.wx.listener;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Created by xuanlubin on 2017/3/24.
 * 初始化组件
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private ClassPathXmlApplicationContext ctx;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        this.ctx = new ClassPathXmlApplicationContext("classpath*:application.xml");
        this.ctx.start();
        event.getServletContext().setAttribute(AppContextListener.class.getName(), this.ctx);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        this.ctx.stop();
    }
}
