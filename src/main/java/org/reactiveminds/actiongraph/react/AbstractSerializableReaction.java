package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.react.http.RestResponse;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractSerializableReaction extends AbstractReaction implements Externalizable {
    private String actionPath;
    public AbstractSerializableReaction(String url, String actionPath) {
        super(url);
        this.actionPath = actionPath;
    }

    private AtomicLong success = new AtomicLong();
    private AtomicLong failure = new AtomicLong();
    public long getSuccessCount(){
        return success.get();
    }
    public long getFailureCount(){
        return failure.get();
    }
    @Override
    protected void onResponse(RestResponse response) {
        if (response.getResponse().isSuccess())
            success.incrementAndGet();
        else {
            failure.incrementAndGet();
        }
    }
    @Override
    protected void onIOError(String event, Throwable cause) {
        super.onIOError(event, cause);
        failure.incrementAndGet();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(endpoint);
        out.writeUTF(actionPath);
        out.writeLong(System.currentTimeMillis());
        out.writeLong(success.get());
        out.writeLong(failure.get());
    }

    public byte[] serialize() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream obj = new ObjectOutputStream(out)) {
            writeExternal(obj);
            obj.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deserialize(byte[] b){
        try(ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b))){
            readExternal(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Override
    public void readExternal(ObjectInput in) throws IOException {
        endpoint = in.readUTF();
        actionPath = in.readUTF();
        in.readLong();
        success = new AtomicLong(in.readLong());
        failure = new AtomicLong(in.readLong());
    }
}
