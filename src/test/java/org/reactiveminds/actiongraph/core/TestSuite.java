package org.reactiveminds.actiongraph.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.reactiveminds.actiongraph.ActionGraphs;

@RunWith(BlockJUnit4ClassRunner.class)
public class TestSuite {
    @Test
    public void testCreateDirectoryPath(){
        ActionGraphs actionGraph = ActionGraphs.instance();
        Group node = actionGraph.createGroup("/mnt/apps/app1");
        Assert.assertNotNull(node);
        Assert.assertEquals("app1", node.name());
        Assert.assertNotNull(node.parent());
        node = node.parent();
        Assert.assertEquals("apps", node.name());
        Assert.assertNotNull(node.parent());
        node = node.parent();
        Assert.assertEquals("mnt", node.name());
        Assert.assertNull(node.parent());
    }
    @Before
    public void before() throws InterruptedException {
        Thread.sleep(1000);
    }

    @Test
    public void testCreateAndChangeDirectories(){
        ActionGraphs actionGraph = ActionGraphs.instance();
        Group app1 = actionGraph.createGroup("/mnt/apps/app1");
        Assert.assertNotNull(app1);
        Assert.assertEquals(1, actionGraph.createGroup("/mnt/apps").list().size());
        Group app2 = app1.parent().changeGroup("app2", true);
        Assert.assertEquals(2, actionGraph.createGroup("/mnt/apps").list().size());
    }
    @Test
    public void testCreateFilePath(){
        ActionGraphs actionGraph = ActionGraphs.instance();
        Action node = actionGraph.createAction("/mnt/apps/app1/", "file.txt");
        Assert.assertNotNull(node);
        Assert.assertEquals("file.txt", node.name());
        Assert.assertNotNull(node.parent());
        Group dir = node.parent();
        Assert.assertEquals("app1", dir.name());

    }
    @Test
    public void testCreateFilePathAndDelete(){
        ActionGraphs actionGraph = ActionGraphs.instance();
        Action node = actionGraph.createAction("/mnt/apps/app1/", "file.txt");
        Assert.assertNotNull(node);
        Assert.assertEquals("file.txt", node.name());
        Assert.assertNotNull(node.parent());
        Group dir = node.parent();
        Assert.assertEquals("app1", dir.name());
        Group node1 = actionGraph.createGroup("/mnt/apps/app1/");
        Assert.assertNotNull(node1);
        Assert.assertFalse(node1.list().isEmpty());
        node.delete();
        Assert.assertFalse(node.exists());
        Assert.assertNull(node.parent());
        Assert.assertTrue(node1.list().isEmpty());
    }
    @Test
    public void testCreateDirectoryPathAndDelete(){
        ActionGraphs actionGraph = ActionGraphs.instance();
        Group node = actionGraph.createGroup("/mnt/apps/app1/");
        node.makeGroup("c1", true).makeGroup("c2", true);
        Group mnt = actionGraph.createGroup("/mnt");
        Assert.assertEquals(1, mnt.children.size());
        node.parent().delete();
        Assert.assertEquals(0, mnt.children.size());
    }
}
