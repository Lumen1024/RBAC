import org.example.CustomDI;
import org.example.RBACSystem;
import org.example.command.CommandRegistry;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;

void main() {
    CustomDI.getRbacSystem().init();
    var parser = CustomDI.getCommandParser();
    CommandRegistry.registerUserCommands(parser);
    CommandRegistry.registerRoleCommands(parser);
    CommandRegistry.registerAssignmentCommands(parser);
    CommandRegistry.registerPermissionCommands(parser);
    CommandRegistry.registerAdditionalCommands(parser);

    var scanner = new Scanner(System.in);
    while (true) {
        var input = scanner.nextLine();
        if (input.toLowerCase().contains("exit"))
            return;
        parser.executeCommand(input, scanner);
    }
}
