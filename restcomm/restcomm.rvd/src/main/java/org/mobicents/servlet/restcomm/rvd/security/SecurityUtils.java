package org.mobicents.servlet.restcomm.rvd.security;


import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;

public class SecurityUtils {

    public static boolean userCanAccessProject(String username, ProjectState project) {
        // by default, projects that belong to no user are accessible by all users
        if (project.getHeader().getOwner() == null)
            return true;

        if (project.getHeader().getOwner().equals(username))
            return true;
        else
            return false;
    }

}
