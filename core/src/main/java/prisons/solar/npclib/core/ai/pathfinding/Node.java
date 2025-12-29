package prisons.solar.npclib.core.ai.pathfinding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.world.BlockPosition;

public class Node implements Comparable<Node> {
    public BlockPosition position;
    public Node parent;
    public double g;
    public double h;
    public double f;

    public Node(){
        this.position = null;
        this.parent = null;
        this.g = 0;
        this.h = 0;
        this.f = 0;
    }

    public Node(@NotNull BlockPosition position, @Nullable Node parent, double g, double h){
        this.position = position;
        this.parent = parent;
        this.g = g;
        this.h = h;
        this.f = g + h;
    }

    public Node reset(@NotNull BlockPosition position, @Nullable Node parent, double g, double h) {
        this.position = position;
        this.parent = parent;
        this.g = g;
        this.h = h;
        this.f = g + h;
        return this;
    }

    @Override
    public int compareTo(@NotNull Node other) {
        int cmp = Double.compare(this.f, other.f);
        if (cmp == 0) {
            return Double.compare(other.h, this.h);
        }
        return cmp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node other)) return false;
        return position.equals(other.position);
    }

    @Override
    public int hashCode(){
        return position.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Node{pos=%s, g=%.2f, h=%.2f, f=%.2f}", position, g, h ,f);
    }
}
