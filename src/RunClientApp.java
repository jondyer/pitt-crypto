/**
 * This class does all the things.
 * Basic CLI for performing file and group operations.
 */
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

// Driver Class
public class RunClientApp {
  public static void main(String [] args) {
    ClientApp newApp = new ClientApp();
  }
}

class ClientApp {

  Scanner console = new Scanner(System.in);
  GroupClient groupClient = new GroupClient();

  public ClientApp(){
    run();
  }
  public void run(){

    // Connect to Server
    groupClient.connect("localhost", 8765);

    // Get Username & Token
    System.out.print("Welcome! Please login with your username >> ");
    String username = console.next();
    UserToken token = groupClient.getToken(username);

    // Check to make sure token exists
    if(token == null) {
      System.out.println("Account not valid.");
      System.exit(0);
    }
    boolean selectGroup = true;
    while(selectGroup){
      // Check if user has admin privileges
      boolean isAdmin = false;
      if(groupClient.isAdmin(username)) {
        System.out.print("Are you performing administrative operations? (y/n) >> ");
        String response = console.next();

        // Wanna be a BIG boy?
        if(response.equals("y") || response.equals("Y"))
        isAdmin = true;
      }

      // Get groups belonged to
      List<List<String>> groupLists = groupClient.listGroups(username, token);
      ArrayList<String> groupsBelongedTo = (ArrayList<String>) groupLists.get(0);
      ArrayList<String> groupsOwned = (ArrayList<String>) groupLists.get(1);

      // List groups
      System.out.println("These are the groups you belong to: ");
      for(int i=0; i<groupsBelongedTo.size(); i++)
      System.out.println(i + ") " + groupsBelongedTo.get(i));

      // TODO: User should be able to create a group if they don't belong to one
      // Select a group
      System.out.print("Please select a group you wish to access ('q' to quit) >> ");
      String selection = console.next();
      if(selection.equals("q")) {
        selectGroup = false;
        break;
      }
      String choice = groupsBelongedTo.get(Integer.parseInt(selection));
      boolean isOwner = false;

      // Check if owner of selected group
      if(groupsOwned.contains(choice) && !isAdmin) {
        System.out.println("Would you to perform owner actions? (y/n) >> ");
        String response = console.next();

        // Wanna be a big boy?
        if(response.equals("y") || response.equals("Y"))
        isOwner = true;
      }


      // Compile List of privileges for each level of usage
      ArrayList<String> adminList = new ArrayList<String>();
      adminList.add("Create user");
      adminList.add("Delete user");
      ArrayList<String> ownerList = new ArrayList<String>();
      ownerList.add("List members of a group");
      ownerList.add("Add user to group");
      ownerList.add("Remove user from group");
      ownerList.add("Delete group");
      ArrayList<String> userList = new ArrayList<String>();
      userList.add("List files");
      userList.add("Upload files");
      userList.add("Download files");
      userList.add("Delete files");
      userList.add("Create a group");

      boolean doAgain = true;
      while(doAgain) {
        // Menu, show selected group and access level
        System.out.println("\n\n----MENU----");
        System.out.println("Selected Group: " + choice);
        if(isAdmin){
          System.out.println("Operating as Admin");
        } else if(isOwner){
          System.out.println("Operating as Owner");
        } else {
          System.out.println("Operating as User");
        }
        System.out.println("\n");

        // List options for each privilege level
        if(isAdmin){
          System.out.println("Admin Ops:");
          for(int i = 0; i < adminList.size(); i++)
          System.out.println("a" + i + ") " + adminList.get(i));
          System.out.println("\n");
        }
        if(isOwner){
          System.out.println("Owner Ops:");
          for(int i = 0; i < ownerList.size(); i++)
          System.out.println("o" + i + ") " + ownerList.get(i));
          System.out.println("\n");
        }
        System.out.println("User Ops:");
        for(int i = 0; i < userList.size(); i++)
        System.out.println(i + ") " + userList.get(i));
        System.out.println("\n");

        System.out.print("Please select an option ('q' to select a different group) >> ");
        String response = console.next();
        switch(response){

          // ADMIN ACTIONS -----------------
          case "a0":
          // Create user
          if(isAdmin) createUser(token);
          break;
          case "a1":
          // Delete user
          if(isAdmin) deleteUser(token);
          break;

          // OWNER ACTIONS -----------------
          case "o0":
          // List members of a group
          if(isOwner) listMembers(choice, token);
          break;
          case "o1":
          // Add user to a group
          if(isOwner) addUserToGroup(choice, token);
          break;
          case "o2":
          // Remove user from a group
          if(isOwner) removeUserFromGroup(choice, token);
          break;
          case "o3":
          // Delete group
          if(isOwner) deleteGroup(choice, token);
          break;

          // USER ACTIONS -----------------
          case "0":
          // List files
          break;
          case "1":
          // Upload files
          break;
          case "2":
          // Download files
          break;
          case "3":
          // Delete files
          break;
          case "4":
          // Create a group=
          break;
          case "q":
          //quit
          doAgain = false;
          break;
          default:
          // Invalid choice
          System.out.println("Not a valid menu choice");
          break;
        }
      } // end doAgain
    } // end selectGroup
    groupClient.disconnect();
  } // end run()

  /**
  * Creates a user in the system (ADMIN ONLY)
  * @param  UserToken myToken       Token of the administrator
  * @return           Success of operation
  */
  public boolean createUser(UserToken myToken) {
    System.out.print("Username of the person you wish to create? >> ");
    String username = console.next();
    boolean status = groupClient.createUser(username, myToken);
    if(status)
      System.out.println("Successfully created user '" + username + "'\n");
    else
      System.out.println("Failed to create user '" + username + "'\n");
    return status;
  }

  /**
  * Deletes a user from the system (ADMIN ONLY)
  * @param  UserToken myToken       Token of the administrator
  * @return           Success of operation
  */
  public boolean deleteUser(UserToken myToken) {
    System.out.print("Username of the person you wish to delete? >> ");
    String username = console.next();
    boolean status = groupClient.deleteUser(username, myToken);
    if(status)
      System.out.println("Successfully deleted user '" + username + "'\n");
    else
      System.out.println("Failed to delete user '" + username + "'\n");
    return status;
  }

  /**
   * Lists all members of a group.
   * @param  String    group         Name of the group to list members for
   * @param  UserToken myToken       Token of the owner of the group
   */
  public void listMembers(String group, UserToken myToken) {
    ArrayList<String> members = (ArrayList<String>) groupClient.listMembers(group, myToken);
    System.out.println("Members of '" + group + "'");
    for(String member : members)
      System.out.println("- " + member);
  }

  /**
   * Adds an existing user to a specfied group.
   * @param  String    group         Name of group to add user to
   * @param  UserToken myToken       Token of the owner of the group
   * @return           Success of operation
   */
  public boolean addUserToGroup(String group, UserToken myToken) {
    System.out.print("Username of the person you wish to add to '" + group +  "'? >> ");
    String username = console.next();
    boolean status = groupClient.addUserToGroup(username, group, myToken);
    if(status)
      System.out.println("Successfully added user '" + username + "' to '"+ group + "'\n");
    else
      System.out.println("Failed to add user '" + username + "'\n");
    return status;
  }

  /**
   * Removes an existing user from a specified group.
   * @param  String    group         Name of group to remove user from
   * @param  UserToken myToken       Token of the owner of the group
   * @return           Success of operation
   */
  public boolean removeUserFromGroup(String group, UserToken myToken) {
    System.out.print("Username of the person you wish to remove from '" + group +  "'? >> ");
    String username = console.next();
    boolean status = groupClient.deleteUserFromGroup(username, group, myToken);
    if(status)
      System.out.println("Successfully removed user '" + username + "' from '"+ group + "'\n");
    else
      System.out.println("Failed to remove user '" + username + "'\n");
    return status;
  }

  /**
   * Removes all user(s) from selected group and deletes the group
   * @param  String    group         Selected group to delete
   * @param  UserToken myToken       Token of the owner of the group to be deleted
   * @return           Success of operation
   */
  public boolean deleteGroup(String group, UserToken myToken) {
    System.out.print("Are you sure you wish to delete group '" + group + "' and remove all users from it? (y/n) >> ");
    String choice = console.next();
    if(choice.equals("Y") || choice.equals("y")) {
      boolean status = groupClient.deleteGroup(group, myToken);
      if(status)
        System.out.println("Successfully deleted group '" + group + "'\n");
      else
        System.out.println("Failed to delete group '" + group + "'\n");
      return status;
    }
    return false;
  }
}