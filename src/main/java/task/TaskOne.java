package task;

import common.CommonContext;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskOne implements Task {
    @Override
    public void execute(CommonContext commonContext) {
        System.out.println("Executing RPC: " + this.getClass().getName());
    }
}