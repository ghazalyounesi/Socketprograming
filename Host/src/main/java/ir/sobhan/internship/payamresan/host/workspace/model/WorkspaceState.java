package ir.sobhan.internship.payamresan.host.workspace.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class WorkspaceState {
    private int port;
    private String creatorPhone;
    private ConcurrentHashMap<String, List<Message>> conversations;
    private ConcurrentHashMap<String, AtomicInteger> sequenceCounters;
    private ConcurrentHashMap<String, Integer> lastReadSequence;
}
