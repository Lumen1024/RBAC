import org.example.CustomDI;
import org.example.RBACSystem;
import org.example.command.CommandRegistry;
import org.example.managers.AssignmentManager;
import org.example.managers.RoleManager;
import org.example.managers.UserManager;
import org.example.utils.ConsoleUtils;

void main() {
    CustomDI.getRbacSystem().init();
    CustomDI.getCleanupService().start(60);
    var parser = CustomDI.getCommandParser();

    CommandRegistry.registerUserCommands(parser);
    CommandRegistry.registerRoleCommands(parser);
    CommandRegistry.registerAssignmentCommands(parser);
    CommandRegistry.registerPermissionCommands(parser);
    CommandRegistry.registerAdditionalCommands(parser);

    var scanner = new Scanner(System.in);

    var username = ConsoleUtils.promptString(scanner, "Введите имя пользователя:", true);
    CustomDI.getRbacSystem().setCurrentUser(username);
    ConsoleUtils.printSuccess("Добро пожаловать, " + username + "!");

    while (true) {
        var input = ConsoleUtils.promptString(scanner, ">>", true);
        if (input.toLowerCase().contains("exit")) {
            CustomDI.getCleanupService().stop();
            ConsoleUtils.printSuccess("Выход из программы");
            return;
        }
        parser.executeCommand(input, scanner);
    }
}
