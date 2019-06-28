package org.apache.rocketmq.client;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by borderx on 2018/9/29.
 */
public class MyTest {

    @Test
    public void testConsumer1() throws MQClientException, UnsupportedEncodingException, RemotingException, InterruptedException {
        // Instantiate with specified consumer group name.
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("my_test_group1");
        // Specify name server addresses.
        consumer.setNamesrvAddr("10.41.32.128:9876");
        // Subscribe one more more topics to consume.
        consumer.subscribe("my_test_topic1", "*");
        // Register callback to execute on arrival of messages fetched from brokers.
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //Launch the consumer instance.
        consumer.start();
        System.out.println("Consumer Started.");
        new Scanner(System.in).nextLine();
    }

    @Test
    public void testConsumer2() throws MQClientException, UnsupportedEncodingException, RemotingException, InterruptedException {
        // Instantiate with specified consumer group name.
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("my_test_group2");
        // Specify name server addresses.
        consumer.setNamesrvAddr("10.41.32.128:9876");
        // Subscribe one more more topics to consume.
        consumer.subscribe("my_test_topic1", "*");
        // Register callback to execute on arrival of messages fetched from brokers.
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //Launch the consumer instance.
        consumer.start();
        System.out.println("Consumer Started.");
        new Scanner(System.in).nextLine();
    }

    @Test
    public void testProducer() throws MQClientException, UnsupportedEncodingException, RemotingException, InterruptedException, MQBrokerException {
        DefaultMQProducer producer = new DefaultMQProducer("my_test_group0");
        // Specify name server addresses.
        producer.setNamesrvAddr("10.41.32.128:9876");
        //Launch the instance.
        producer.start();
        for (int i = 0; i < 10; i++) {
            //Create a message instance, specifying topic, tag and message body.
            Message msg = new Message("my_test_topic1", "TagA", ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
            //Call send message to deliver message to one of brokers.
            SendResult sendResult = producer.send(msg);
            System.out.printf("%s%n", sendResult);
        }
        //Shut down once the producer instance is not longer in use.
        producer.shutdown();
    }

    @Test
    public void testAsynchronouslyProducer() {
        //Instantiate with a producer group name.
        DefaultMQProducer producer = new DefaultMQProducer("my_test_group1");
        // Specify name server addresses.
        producer.setNamesrvAddr("10.41.32.128:9876");
        //Launch the instance.
        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        producer.setRetryTimesWhenSendAsyncFailed(0);
        for (int i = 0; i < 100; i++) {
            final int index = i;
            //Create a message instance, specifying topic, tag and message body.
            Message msg = null;
            try {
                msg = new Message("my_test_topic1",
                        "TagA",
                        "OrderID188",
                        "Hello world".getBytes(RemotingHelper.DEFAULT_CHARSET));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                producer.send(msg, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        System.out.printf("%-10d OK %s %n", index,
                                sendResult.getMsgId());
                    }
                    @Override
                    public void onException(Throwable e) {
                        System.out.printf("%-10d Exception %s %n", index, e);
                        e.printStackTrace();
                    }
                });
            } catch (MQClientException e) {
                e.printStackTrace();
            } catch (RemotingException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("producer Started.%n");
        new Scanner(System.in).nextLine();
        //Shut down once the producer instance is not longer in use.
        producer.shutdown();
    }

    @Test
    public void testOrderProducer() throws MQClientException, UnsupportedEncodingException, RemotingException, InterruptedException, MQBrokerException {
        //Instantiate with a producer group name.
        DefaultMQProducer producer = new DefaultMQProducer("my_test_group2");
        producer.setNamesrvAddr("10.41.32.128:9876");
        //Launch the instance.
        producer.start();
        String[] tags = new String[]{"TagA", "TagB", "TagC", "TagD", "TagE"};
        for (int i = 0; i < tags.length; i++) {
            for (int j = 0; j < 10; j++) {
                Message msg = new Message("my_test_topic3", tags[i], "KEY" + j,
                        ("Hello RocketMQ " + j).getBytes(RemotingHelper.DEFAULT_CHARSET));
                SendResult sendResult = producer.send(msg, new MessageQueueSelector() {
                    @Override
                    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                        Integer id = (Integer) arg;
                        int index = id % mqs.size();
                        return mqs.get(index);
                    }
                }, j);
                System.out.printf("%s%n", sendResult);
            }
        }
        //server shutdown
        producer.shutdown();
    }

    @Test
    public void testOrderConsumer() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("my_test_group3");
        consumer.setNamesrvAddr("10.41.32.128:9876");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe("my_test_topic3", "*");

        consumer.registerMessageListener(new MessageListenerOrderly() {

            AtomicLong consumeTimes = new AtomicLong(0);
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                                       ConsumeOrderlyContext context) {
                context.setAutoCommit(false);
                try {
                    System.out.printf(Thread.currentThread().getName() + " Receive New Messages: " + msgs + "%n " + new String(msgs.get(0).getBody(), RemotingHelper.DEFAULT_CHARSET) + "%n");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                this.consumeTimes.incrementAndGet();
//                if ((this.consumeTimes.get() % 2) == 0) {
//                    return ConsumeOrderlyStatus.SUCCESS;
//                } else if ((this.consumeTimes.get() % 5) == 0) {
//                    context.setSuspendCurrentQueueTimeMillis(3000);
//                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
//                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });

        consumer.start();

        System.out.printf("Consumer Started.%n");
        new Scanner(System.in).nextLine();
    }



}
