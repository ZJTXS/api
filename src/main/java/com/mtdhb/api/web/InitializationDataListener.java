package com.mtdhb.api.web;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.mtdhb.api.constant.e.ReceivingStatus;
import com.mtdhb.api.constant.e.ThirdPartyApplication;
import com.mtdhb.api.dao.ReceivingRepository;
import com.mtdhb.api.entity.Receiving;
import com.mtdhb.api.service.AsyncService;
import com.mtdhb.api.service.CookieService;
import com.mtdhb.api.service.TimesService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author i@huangdenghe.com
 * @date 2018/06/05
 */
@Component
@Slf4j
public class InitializationDataListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private AsyncService asyncService;
    @Autowired
    private CookieService cookieService;
    @Autowired
    private TimesService timesService;
    @Autowired
    private ReceivingRepository receivingRepository;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // root of context hierarchy
        if (event.getApplicationContext().getParent() == null) {
            log.info("Initializing custom");
            Stream.of(ThirdPartyApplication.values()).forEach(application -> cookieService.load(application));
            // 执行未完成的领取任务
            List<Receiving> receivings = receivingRepository.findByStatus(ReceivingStatus.ING);
            log.info("receivings#size={}", receivings.size());
            receivings.stream().forEach(receiving -> {
                long available = timesService.getAvailable(receiving.getApplication(), receiving.getUserId());
                asyncService.dispatch(receiving, available);
            });
        }
    }

}
