package com.bidhub.client.network;


import javafx.concurrent.Task;

import java.util.concurrent.Callable;

/**
 * Chống đơ màn hình
 * Wrapper {@link Task} cho mọi network call — đảm bảo không block FX thread.
 *
 * <p>Cách dùng trong Controller:
 * <pre>
 *   NetworkTask&lt;MessageResponse&gt; task = new NetworkTask&lt;&gt;(() -&gt;
 *       ServerGateway.getInstance().sendRequest(req));
 *
 *   // Không dùng Platform.runLater vì setOnSucceeded đã chạy trên FX Thread
 *   task.setOnSucceeded(e -&gt; handleSuccess(task.getValue()));
 *   task.setOnFailed(e -&gt; showError(task.getException()));
 *
 *   new Thread(task).start();
 * </pre>
 *
 * @param <T> kiểu kết quả trả về
 */
public final class NetworkTask<T> extends Task<T> {
    private final Callable<T> callable;

    /** @param callable logic cần chạy ngoài FX thread */
    public NetworkTask(Callable<T> callable){
        this.callable = callable;
    }

    @Override
    protected T call() throws Exception{
        return callable.call();
    }
}
