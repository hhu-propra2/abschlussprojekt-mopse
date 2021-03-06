package mops.presentation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mops.businesslogic.group.GroupService;
import mops.businesslogic.security.Account;
import mops.exception.MopsException;
import mops.persistence.group.Group;
import mops.presentation.error.ExceptionPresentationError;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller Class for all requests on '/material1/groups'.
 */
@Controller
@RequestMapping("/material1/groups")
@AllArgsConstructor
@Slf4j
// demeter violations in logging
// dataflow/one return violations in try-catch statements
@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.OnlyOneReturn", "PMD.LawOfDemeter" })
public class GroupsController {

    /**
     * Connects to our group database.
     */
    private final GroupService groupService;

    /**
     * Gets all groups of a user.
     *
     * @param redirectAttributes redirect attributes
     * @param token              authentication token from keycloak server.
     * @param model              view model.
     * @return groups view
     */
    @GetMapping
    public String getAllGroups(RedirectAttributes redirectAttributes,
                               KeycloakAuthenticationToken token,
                               Model model) {
        Account account = Account.of(token);
        log.info("All groups requested for user '{}'.", account.getName());

        try {
            List<Group> groups = groupService.getUserGroups(account);
            model.addAttribute("groups", groups);
        } catch (MopsException e) {
            log.error("Failed to retrieve user groups for '{}':", account.getName(), e);
            redirectAttributes.addFlashAttribute("error", new ExceptionPresentationError(e));
            return "redirect:/material1/error";
        }

        model.addAttribute("account", account);
        return "groups";
    }
}
