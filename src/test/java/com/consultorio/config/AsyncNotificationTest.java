package com.consultorio.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AsyncNotificationTest {

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Test
    @DisplayName("El bean taskExecutor debe ejecutar tareas asíncronamente en hilos con prefijo ConsultorioAsync-")
    void testTaskExecutorEjecutaEnHiloSeparado() throws ExecutionException, InterruptedException, TimeoutException {
        String hiloPrincipal = Thread.currentThread().getName();
        CompletableFuture<String> future = new CompletableFuture<>();

        taskExecutor.execute(() -> future.complete(Thread.currentThread().getName()));

        String hiloAsincrono = future.get(5, TimeUnit.SECONDS);

        assertNotEquals(hiloPrincipal, hiloAsincrono, "La tarea debe ejecutarse en un hilo desacoplado del hilo principal.");
        assertTrue(hiloAsincrono.startsWith("ConsultorioAsync-"),
                "El hilo debe tener el prefijo configurado en AsyncConfig ('ConsultorioAsync-'), pero fue: " + hiloAsincrono);
    }
}
