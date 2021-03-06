package org.reactiveminds.actiongraph.store.mapdb;

import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.store.GroupData;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

class GroupDataSer implements Serializer<GroupData>, Serializable {
    @Override
    public void serialize(DataOutput out, GroupData value) throws IOException {
        out.writeUTF(value.getRoot());
        out.writeInt(value.getGraphs().size());
        for (String s : value.getGraphs()) {
            out.writeUTF(s);
        }
    }

    @Override
    public GroupData deserialize(DataInput in, int available) throws IOException {
        GroupData groupData = new GroupData();
        groupData.setRoot(in.readUTF());
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            groupData.getGraphs().add(in.readUTF());
        }
        return groupData;
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}
