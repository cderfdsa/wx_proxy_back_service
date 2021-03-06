package me.robin.wx.service;

import me.robin.wx.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by xuanlubin on 2017/2/14.
 * 公众号任务管理
 */
public enum BizQueueManager {

    INS;

    //最新列表任务
    private BlockingQueue<MutableTriple<String, Long, String>> bizQueue = new LinkedBlockingQueue<>();
    //历史列表任务
    private BlockingQueue<MutableTriple<String, Long, String>> hisBizQueue = new LinkedBlockingQueue<>();
    //下发待检测
    private BlockingQueue<MutableTriple<String, Long, String>> dispatchTime = new LinkedBlockingQueue<>();
    //对象映射
    private Map<String, MutableTriple<String, Long, String>> pairMapping = new ConcurrentHashMap<>();

    //60秒
    final Long LIMIT = 60000L;

    BizQueueManager() {
        new Thread(() -> {
            while (true) {
                try {
                    MutableTriple<String, Long, String> triple = dispatchTime.take();
                    if (triple.getMiddle() + LIMIT < System.currentTimeMillis()) {
                        if (StringUtils.isNotBlank(triple.getRight())) {
                            hisBizQueue.offer(triple);
                        } else {
                            bizQueue.offer(triple);
                        }
                    } else {
                        dispatchTime.offer(triple);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public MutableTriple<String, Long, String> fetchNextBiz(boolean history) {
        MutableTriple<String, Long, String> triple = bizQueue.poll();
        if (null == triple && history) {
            triple = hisBizQueue.poll();
        }
        if (null != triple) {
            triple.setMiddle(System.currentTimeMillis());
            dispatchTime.offer(triple);
            pairMapping.put(triple.getLeft() + "#" + triple.getRight(), triple);
        }

        //如果当前客户端cookie失效并且没有获取到首页任务 则下发默认任务用来刷新当前客户端的cookie
        if (!history && null == triple) {
            return new MutableTriple<>(Constants.REFRESH_BIZ, null, null);
        }

        return triple;
    }

    public void offerNewTask(String biz) {
        offerNewTask(biz, null);
    }

    public void report(String biz, String fromMsgId) {
        MutableTriple<String, Long, String> triple = pairMapping.remove(biz + "#" + fromMsgId);
        if (null != triple) {
            dispatchTime.remove(triple);
        }
    }

    public void offerNewTask(String biz, String fromMsgId) {
        if (StringUtils.isBlank(biz)) {
            return;
        }
        if (StringUtils.isNotBlank(fromMsgId)) {
            hisBizQueue.offer(new MutableTriple<>(biz, null, fromMsgId));
        } else {
            bizQueue.offer(new MutableTriple<>(biz, null, fromMsgId));
        }
    }


    public boolean isEmpty() {
        return bizQueue.isEmpty() && hisBizQueue.isEmpty() && dispatchTime.isEmpty();
    }
}