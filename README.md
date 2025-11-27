# 🚀 **Redis + Spring Boot — Complete From-Scratch Guide**

This guide covers:

- Redis basics
- Spring Boot integration
- Redis caching strategies (Read-Through, Write-Through, Cache-Aside, Write-Behind, TTL, Invalidation)
- Distributed locks
- Rate limiting
- Redis data types
- Short code samples everywhere

---

# 1️⃣ **What is Redis?**

Redis (`REmote DIctionary Server`) is an **in-memory key-value store** used for:

- Caching
- High-speed reads/writes
- Distributed locks
- Rate limiting
- Pub/Sub
- Session storage

Its speed comes from staying in **RAM**, not disk.

---

# 2️⃣ **Add Redis in Spring Boot**

### **Dependencies**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### **Enable Caching**

```java
@SpringBootApplication
@EnableCaching
public class App {}
```

### **application.yml**

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
  cache:
    redis:
      time-to-live: 60000  # default TTL for cacheable entries
```

---

# 3️⃣ **Redis Caching Strategies — In-Depth + Code**

## 🔥 1. **Read-Through Cache (Lazy Loading)**

**Most common and used via `@Cacheable`.**

### **What happens**

- App checks Redis first
- If **hit**, return cached value
- If **miss**, call DB → store in Redis → return

### **Why**

- Super-fast reads
- Removes duplicate DB queries
- Cache auto-populates only when needed

### **Code**

```java
@Cacheable(value = "product", key = "#id")
public Product getProduct(Long id) {
    return repo.findById(id).orElseThrow(); // executed only on cache MISS
}
```

---

## 🔥 2. **Write-Through Cache**

Use `@CachePut` to **update DB + Redis simultaneously**.

### **What happens**

- App updates DB
- Spring also writes new value into Redis
- Cache always stays fresh

### **Why**

- Ensures Redis never returns stale data
- Good for read-heavy applications

### **Code**

```java
@CachePut(value = "product", key = "#product.id")
public Product update(Product product) {
    return repo.save(product);
}
```

---

## 🔥 3. **Write-Behind / Write-Back**

Writes go to Redis first → Redis writes to DB **asynchronously**.

### **What happens**

- Immediate write to Redis
- A worker later flushes updates to DB in batches

### **Why**

- Super-fast writes
- Perfect for analytics, logs, counters

### **Downside**

- Possible data loss if Redis crashes before flush

### **Code Idea (simple version)**

```java
redis.opsForList().leftPush("pending-orders", json);
// worker reads list and persists to DB later
```

---

## 🔥 4. **Cache-Aside Pattern (Manual Control)**

Used by big systems: Netflix, Uber, Amazon.

### **What happens**

- Developer manually checks Redis
- If miss → fetch from DB → write to Redis
- Gives full flexibility over TTL, invalidation, structure

### **Why**

- Best for distributed microservices
- Custom keys, custom expiry, custom formats

### **Code**

```java
public User getUser(Long id) {
    String key = "user:" + id;
    String json = redis.opsForValue().get(key);

    if (json != null)
        return mapper.readValue(json, User.class);

    User user = repo.findById(id).orElseThrow();
    redis.opsForValue().set(key, mapper.writeValueAsString(user), 60, TimeUnit.SECONDS);

    return user;
}
```

---

## 🔥 5. **TTL (Time-To-Live) Expiry Strategy**

Every cached entry expires automatically after a time limit.

### **Why**

- Ensures freshness
- Prevents stale cache
- Saves memory

### **Spring Boot TTL**

```yaml
spring:
  cache:
    redis:
      time-to-live: 60000 # 60 seconds
```

### **Manual TTL**

```java
redis.opsForValue().set("user:1", json, 30, TimeUnit.SECONDS);
```

---

## 🔥 6. **Cache Invalidation Strategies**

Stale data must be removed when DB changes.

### **Types**

### **A. Manual Invalidation**

```java
redis.delete("product:1");
```

### **B. Auto Invalidation (`@CacheEvict`)**

```java
@CacheEvict(value = "product", key = "#id")
public void deleteProduct(Long id) {
    repo.deleteById(id);
}
```

### **C. Clear all cache**

```java
@CacheEvict(value = "product", allEntries = true)
public void clearAll() {}
```

### **Why**

- Ensures consistency between DB and Redis
- Prevents returning stale/incorrect values

---

# 4️⃣ **Pairing Strategies in Real Projects**

### **Most standard setup**

✔ `@Cacheable` → Read-Through
✔ `@CachePut` → Write-Through
✔ `@CacheEvict` → Invalidation

This ensures:

- Fast reads
- Accurate results
- No stale data

---

# 5️⃣ **Distributed Locking**

Prevent simultaneous updates in multi-instance apps.

### **Ideal for**

- Preventing double order submission
- Preventing race conditions

### **Code**

```java
Boolean lock = redis.opsForValue().setIfAbsent("lock:task1", "1", 10, TimeUnit.SECONDS);

if (Boolean.FALSE.equals(lock))
    return;   // someone else has lock

try {
    // critical section
} finally {
    redis.delete("lock:task1");
}
```

---

# 6️⃣ **Rate Limiting (Requests per Minute)**

### **Why**

- Protect APIs
- Avoid abuse
- Control traffic

### **Code**

```java
public boolean allow(String userId) {
    Long count = redis.opsForValue().increment("rate:" + userId);

    if (count == 1)
        redis.expire("rate:" + userId, Duration.ofSeconds(60));

    return count <= 100; // 100 requests per minute
}
```

---

# 7️⃣ **Pub/Sub — Realtime Messaging**

### **Use cases**

- Notification systems
- Chat apps
- Event broadcasting

### **Publish**

```java
redis.convertAndSend("notifications", "Order shipped!");
```

---

# 8️⃣ **Redis Data Types (with minis examples)**

| Type           | Use Case                           | Spring API    |
| -------------- | ---------------------------------- | ------------- |
| **String**     | Caching objects, tokens            | opsForValue() |
| **Hash**       | Storing fields for a single object | opsForHash()  |
| **List**       | Queues, logs                       | opsForList()  |
| **Set**        | Unique items                       | opsForSet()   |
| **Sorted Set** | Leaderboards, rankings             | opsForZSet()  |

### **Examples**

#### String

```java
redis.opsForValue().set("token:1", "abc123");
```

#### Hash

```java
redis.opsForHash().put("user:1", "name", "Waleed");
```

#### List

```java
redis.opsForList().rightPush("logs", "event1");
```

#### Set

```java
redis.opsForSet().add("tags", "spring");
```

#### Sorted Set

```java
redis.opsForZSet().add("ranking", "waleed", 90);
```

---

# 9️⃣ **Summary Table (Quick Revision)**

| Strategy             | When Used                   | Pros          | Cons              |
| -------------------- | --------------------------- | ------------- | ----------------- |
| **Read-Through**     | Lazy load; most APIs        | Fast reads    | First read slow   |
| **Write-Through**    | Update both DB & cache      | No stale data | Writes slower     |
| **Write-Behind**     | High-speed writes           | Very fast     | Data loss risk    |
| **Cache-Aside**      | Microservices; fine control | Most flexible | Manual coding     |
| **TTL**              | Time-based freshness        | Auto cleanup  | Expiry tuning     |
| **Invalidation**     | Delete stale data           | Consistency   | Must handle cases |
| **Distributed Lock** | Avoid concurrency issues    | Safe updates  | Extra coding      |
| **Rate Limit**       | Protect endpoints           | Easy logic    | Needs tuning      |
| **Pub/Sub**          | Realtime events             | Simple        | Not persistent    |
