package org.reactiveminds.actiongraph.store;

import org.mapdb.Serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

class GraphDataSer implements Serializer<GroupData>, Serializable {
    @Override
    public void serialize(DataOutput out, GroupData value) throws IOException {
        out.writeInt(value.getJsonContents().size());
        for (String s : value.getJsonContents()) {
            out.writeUTF(s);
        }
    }

    @Override
    public GroupData deserialize(DataInput in, int available) throws IOException {
        GroupData groupData = new GroupData();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            groupData.getJsonContents().add(in.readUTF());
        }
        return groupData;
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}
