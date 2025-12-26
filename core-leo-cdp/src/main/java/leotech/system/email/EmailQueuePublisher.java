package leotech.system.email;


import leotech.cdp.model.marketing.EmailMessage;
import leotech.system.util.RedisPubSubClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;

public class EmailQueuePublisher {

    private static final String CHANNEL = "leocdp_email_queue";
    private final JedisPool jedisPool;

    public EmailQueuePublisher() {
        this.jedisPool = RedisClientFactory
                .buildRedisPool(RedisPubSubClient.PUB_SUB_QUEUE_REDIS);
    }

    public void publish(EmailMessage emailMessage) {
        final String payload = emailMessage.toString();

        new RedisCommand<Long>(jedisPool) {
            @Override
            protected Long build(Jedis jedis) throws JedisException {
                return jedis.publish(CHANNEL, payload);
            }
        }.executeAsync();
    }
}
