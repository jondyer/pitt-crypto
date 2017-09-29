/* This thread does all the work. It communicates with the client through Envelopes.
 *
 */
import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.List;
import java.util.*;

public class GroupThread extends Thread
{
	private final Socket socket;
	private GroupServer my_gs;

	public GroupThread(Socket _socket, GroupServer _gs)
	{
		socket = _socket;
		my_gs = _gs;
	}

	public void run()
	{
		boolean proceed = true;

		try
		{
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

			do
			{
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());
				Envelope response;

				if(message.getMessage().equals("GET"))//Client wants a token
				{
					String username = (String)message.getObjContents().get(0); //Get the username
					if(username == null)
					{
						response = new Envelope("FAIL");
						response.addObject(null);
						output.writeObject(response);
					}
					else
					{
						UserToken yourToken = createToken(username); //Create a token

						//Respond to the client. On error, the client will receive a null token
						response = new Envelope("OK");
						response.addObject(yourToken);
						output.writeObject(response);
					}
				}
				else if(message.getMessage().equals("CUSER")) //Client wants to create a user
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								String username = (String)message.getObjContents().get(0); //Extract the username
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								if(createUser(username, yourToken))
								{
									response = new Envelope("OK"); //Success
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DUSER")) //Client wants to delete a user
				{
					response = new Envelope("FAIL");
					if(message.getObjContents().size() >= 2) {
						if(message.getObjContents().get(0) != null)	{
							if(message.getObjContents().get(1) != null)	{
								String username = (String)message.getObjContents().get(0); //Extract the username
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								if(deleteUser(username, yourToken))
									response = new Envelope("OK"); //Success
							}
						}
					}
					output.writeObject(response);
				}
				else if(message.getMessage().equals("CGROUP")) //Client wants to create a group
 				{
					response = new Envelope("FAIL");
					if(message.getObjContents().size() >= 2) {
						if(message.getObjContents().get(0) != null) {
							if(message.getObjContents().get(1) != null) {
								UserToken yourToken = (UserToken) message.getObjContents().get(0); //Extract the token
								String groupName = (String) message.getObjContents().get(1); //Extract the group name
								if(createGroup(groupName, yourToken))
									response = new Envelope("OK"); //Success
							}
						}
					}
					output.writeObject(response);
				}
				else if(message.getMessage().equals("DGROUP")) //Client wants to delete a group
				{
				    /* TODO:  Write this handler */
				}
				else if(message.getMessage().equals("LMEMBERS")) //Client wants a list of members in a group
				{
					response = new Envelope("FAIL");

				    if(message.getObjContents().size() >= 2) {
						if(message.getObjContents().get(0) != null) {
							if(message.getObjContents().get(1) != null) {
								String groupName = (String)message.getObjContents().get(0); //Extract the groupName
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								List<String> members = listMembers(groupName, yourToken);
								if(members.size() > 0) {
									response = new Envelope("OK"); //Success
									response.addObject(members);
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("AUSERTOGROUP")) //Client wants to add user to a group
				{
				    response = new Envelope("FAIL");

					if(message.getObjContents().size() >= 3) {
						if(message.getObjContents().get(0) != null) {
							if(message.getObjContents().get(1) != null) {
								if(message.getObjContents().get(2) != null) {
								String userName = (String)message.getObjContents().get(0); //Extract the username
								String groupName = (String)message.getObjContents().get(1); //Extract the groupName
								UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token

								List<String> members = listMembers(groupName, yourToken);
								if(addUserToGroup(userName, groupName, yourToken))
									response = new Envelope("OK"); //Success
								} // missing token
							} // missing groupName
						} // missing userName
					} // missing something!
					output.writeObject(response);
				}
				else if(message.getMessage().equals("RUSERFROMGROUP")) //Client wants to remove user from a group
				{
				    /* TODO:  Write this handler */
				}
				else if(message.getMessage().equals("DISCONNECT")) //Client wants to disconnect
				{
					socket.close(); //Close the socket
					proceed = false; //End this communication loop
				}
				else
				{
					response = new Envelope("FAIL"); //Server does not understand client request
					output.writeObject(response);
				}
			}while(proceed);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	//Method to create tokens
	private UserToken createToken(String username)
	{
		//Check that user exists
		if(my_gs.userList.checkUser(username))
		{
			//Issue a new token with server's name, user's name, and user's groups
			UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
			return yourToken;
		}
		else
		{
			return null;
		}
	}


	//Method to create a user
	private boolean createUser(String username, UserToken yourToken)
	{
		String requester = yourToken.getSubject();

		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
			//Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administrator
			if(temp.contains("ADMIN"))
			{
				//Does user already exist?
				if(my_gs.userList.checkUser(username))
				{
					return false; //User already exists
				}
				else
				{
					my_gs.userList.addUser(username);
					return true;
				}
			}
			else
			{
				return false; //requester not an administrator
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}

	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken)
	{
		String requester = yourToken.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administer
			if(temp.contains("ADMIN"))
			{
				//Does user exist?
				if(my_gs.userList.checkUser(username))
				{
					//User needs deleted from the groups they belong
					ArrayList<String> deleteFromGroups = new ArrayList<String>();

					//This will produce a hard copy of the list of groups this user belongs
					for(int index = 0; index < my_gs.userList.getUserGroups(username).size(); index++)
					{
						deleteFromGroups.add(my_gs.userList.getUserGroups(username).get(index));
					}

					// TODO: Actually remove user from groups

					//If groups are owned, they must be deleted
					ArrayList<String> deleteOwnedGroup = new ArrayList<String>();

					//Make a hard copy of the user's ownership list
					for(int index = 0; index < my_gs.userList.getUserOwnership(username).size(); index++)
					{
						deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));
					}

					//Delete owned groups
					for(int index = 0; index < deleteOwnedGroup.size(); index++)
					{
						//Use the delete group method. Token must be created for this action
						deleteGroup(deleteOwnedGroup.get(index), new Token(my_gs.name, username, deleteOwnedGroup));
					}

					//Delete the user from the user list
					my_gs.userList.deleteUser(username);

					return true;
				}
				else
				{
					return false; //User does not exist

				}
			}
			else
			{
				return false; //requester is not an administer
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}

	// TODO: Write method
	private boolean deleteGroup(String ownedGroup, UserToken token) {
		return false;
	}

	// TODO: Handle check for duplicate groups
	private boolean createGroup(String groupName, UserToken token) {
		my_gs.userList.addGroup(token.getSubject(), groupName);
		my_gs.userList.addOwnership(token.getSubject(), groupName);
		return true;
	}

	private List<String> listMembers(String group, UserToken token) {
		List<String> members = new ArrayList<String>();
		String requester = token.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester)) {
			// Checks to make sure the requester is the owner of the group
			if (my_gs.userList.getUserOwnership(requester).contains(group)) {

				String[] users = my_gs.userList.getAllUsers();
				for(int i = 0; i < users.length; i++) {
					if (my_gs.userList.getUserGroups(users[i]).contains(group)) {
						members.add(users[i]);
					}
				}
			}

		}

		return members;
	}

	// TODO: Write method
	/**
	 * Adds an extant user to the specified group. Owner of token must also be owner of group.
	 * @param  String    user          The user to add to the group.
	 * @param  String    group         The group to which the user is added.
	 * @param  UserToken token         Token belonging to the group owner.
	 * @return           Whether or not the operation was successful.
	 */
	private boolean addUserToGroup(String user, String group, UserToken token) {
		String requester = token.getSubject();

		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
			//Check if user exists
			if(my_gs.userList.checkUser(user))
			{
				//Get the requester's groups
				ArrayList<String> temp = my_gs.userList.getUserOwnership(requester);
				//requester needs to be group owner
				if(temp.contains(group))
				{
					my_gs.userList.addGroup(user, group);
					return true;
				} else return false; //requester does not own group
			} else return false; //user does not exist
		} else return false; //requester does not exist
	}

	// TODO: Write method
	private boolean deleteUserFromGroup(String ownedGroup, UserToken token) {
		return false;
	}
}