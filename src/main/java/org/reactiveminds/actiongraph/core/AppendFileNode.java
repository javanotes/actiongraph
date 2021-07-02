package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.ActionGraphException;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * @deprecated
 */
class AppendFileNode extends Action {
    AppendFileNode(Group parent, String name, ReadWriteLock readWriteLock) {
        super(parent, name, readWriteLock);
    }
    @Override
    public void write(String content){
        readWriteLock.writeLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("File is deleted!");
            if(this.content == null)
                this.content = content;
            else
                this.content += content;
        }
        finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
