package ir.sobhan.internship.payamresan.host.workspace.model;

import lombok.Getter;
import lombok.Setter;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class HostPersistentState {

    private ConcurrentHashMap<Integer, WorkspaceState> runningWorkspaces;
}
