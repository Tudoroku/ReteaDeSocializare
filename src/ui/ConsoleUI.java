package ui;

import domain.User;
import repository.RepositoryException;
import service.CommunityService;
import service.FriendshipService;
import service.UserService;

import java.util.Scanner;

public class ConsoleUI implements UI{
    private final CommunityService communityService;
    private final FriendshipService friendshipService;
    private final UserService userService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleUI(CommunityService communityService, FriendshipService friendshipService, UserService userService) {
        this.communityService = communityService;
        this.friendshipService = friendshipService;
        this.userService = userService;
    }

    @Override
    public void start()
    {
        boolean running = true;

        while(running)
        {
            drawMenu();

            try
            {
                if(!scanner.hasNextLine())
                {
                    break;
                }

                int command = Integer.parseInt(scanner.nextLine().trim());

                if(command == 0)
                {
                    running = false;
                }
                else
                {
                    handleCommand(command);
                }
            }
            catch(NumberFormatException e)
            {
                System.out.println("Invalid number!");
            }
            catch(IllegalArgumentException | RepositoryException | UnsupportedOperationException e)
            {
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Goodbye!");
    }

    private void drawMenu() {
        System.out.println("========== SOCIAL NETWORK ==========\n");
        System.out.println("1. Add user");
        System.out.println("2. Delete user");
        System.out.println("3. Add friendship");
        System.out.println("4. Delete friendship");
        System.out.println("5. Number of communities");
        System.out.println("6. Most sociable community");
        System.out.println();
        System.out.println("0. Exit");
        System.out.println();
        System.out.println("Choose an option: ");
    }

    private void handleCommand(int command)
    {
        switch(command)
        {
            case 1 -> addUser();
            case 2 -> deleteUser();
            case 3 -> addFriendship();
            case 4 -> deleteFriendship();
            case 5 -> showNumberOfCommunities();
            case 6 -> showMostSociableCommunity();

            default -> System.out.println("Invalid command!");
        }
    }

    private void addUser()
    {
        System.out.print("User ID: ");
        Long id = Long.parseLong(scanner.nextLine().trim());

        System.out.print("First name: ");
        String firstName = scanner.nextLine().trim();

        System.out.print("Last name: ");
        String lastName = scanner.nextLine().trim();

        User user = new User(id, firstName, lastName);
        userService.addUser(user);

        System.out.println("User added successfully!");
    }

    private void deleteUser()
    {
        System.out.print("User ID: ");
        Long id = Long.parseLong(scanner.nextLine().trim());

        userService.deleteUser(id);

        System.out.println("User deleted successfully!");
    }

    private void addFriendship()
    {
        System.out.print("First user ID: ");
        Long id1 = Long.parseLong(scanner.nextLine().trim());

        System.out.print("Second user ID: ");
        Long id2 = Long.parseLong(scanner.nextLine().trim());

        friendshipService.addFriendship(id1, id2);

        System.out.println("Friendship added successfully!");
    }

    private void deleteFriendship()
    {
        System.out.print("First user ID: ");
        Long id1 = Long.parseLong(scanner.nextLine().trim());

        System.out.print("Second user ID: ");
        Long id2 = Long.parseLong(scanner.nextLine().trim());

        friendshipService.deleteFriendship(id1, id2);

        System.out.println("Friendship deleted successfully!");
    }

    private void showNumberOfCommunities()
    {
        int count = communityService.getNumberOfCommunities();

        System.out.println("Number of communities: " + count);
    }

    private void showMostSociableCommunity()
    {
        var community = communityService.getMostSociableCommunity();

        if(community.isEmpty())
        {
            System.out.println("The network is empty!");
            return;
        }

        System.out.println("Most sociable community: " + community);
    }

}
