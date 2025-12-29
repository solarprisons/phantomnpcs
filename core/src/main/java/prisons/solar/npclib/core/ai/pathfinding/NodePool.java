package prisons.solar.npclib.core.ai.pathfinding;

import prisons.solar.npclib.api.world.BlockPosition;

public class NodePool {
    private final Node[] nodes;
    private int index;

    public NodePool(){
        this(10_000);
    }

    public NodePool(int capacity){
        this.nodes = new Node[capacity];
        this.index = 0;

        for (int i = 0; i < capacity; i++) {
            nodes[i] = new Node();
        }
    }

    public Node acquire(BlockPosition pos, Node parent, double g, double h){
        if (index >= nodes.length) {
            throw new IllegalStateException("Node Pool Exhausted! Tried to allocate node " + (index + 1) +
                    " but pool only has " + nodes.length + " nodes. Consider increasing pool size or reducing maxPathLength.");
        }
        Node node = nodes[index++];
        return node.reset(pos, parent, g, h);
    }

    public int size(){ return index; }

    public int capacity() { return nodes.length; }

    public boolean hasCapacity() { return index < nodes.length; }

    public void reset() { index = 0; }


}
