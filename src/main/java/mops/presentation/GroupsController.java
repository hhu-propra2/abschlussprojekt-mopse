package mops.presentation;

import lombok.AllArgsConstructor;
import mops.Account;
import mops.AccountUtil;
import mops.businesslogic.GroupService;
import mops.persistence.Directory;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller Class for all requests on '/material1/groups'.
 */
@Controller
@RequestMapping("/material1/groups")
@AllArgsConstructor
public class GroupsController {

    /**
     * Communicator with 'Gruppenfindung'.
     */
    private final GroupService groupService;


    /**
     * @param token authentication token from keycloak server.
     * @param model view model.
     * @return groups view
     */
    @GetMapping
    public String getAllGroups(KeycloakAuthenticationToken token, Model model) {
        final Account account = AccountUtil.getAccountFromToken(token);
        final List<Directory> groups = groupService.getAllGroups(account);
        model.addAttribute("groups", groups);
        return "groups";
    }

}