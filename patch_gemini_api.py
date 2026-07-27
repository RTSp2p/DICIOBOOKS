import re

with open('app/src/main/java/com/example/network/GeminiApi.kt', 'r') as f:
    content = f.read()

retry_interceptor = """
class RetryInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        val maxLimit = 3

        while (!response.isSuccessful && response.code == 429 && tryCount < maxLimit) {
            tryCount++
            val backoffTime = (1L shl tryCount) * 1000L // 2s, 4s, 8s
            response.close()
            try {
                Thread.sleep(backoffTime)
            } catch (e: InterruptedException) {
                // ignore
            }
            response = chain.proceed(request)
        }
        return response
    }
}

object RetrofitClient {"""

content = content.replace("object RetrofitClient {", retry_interceptor)

client_build = """        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {"""

content = content.replace("        .writeTimeout(60, TimeUnit.SECONDS)\n        .addInterceptor(HttpLoggingInterceptor().apply {", client_build)

with open('app/src/main/java/com/example/network/GeminiApi.kt', 'w') as f:
    f.write(content)
