package tools.vitruv.branchingcasestudy.vsum.util.base;

import java.util.List;

import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.UserInteractionOptions;

/**
 * The interaction result provider used in this case study. It allows the changing of answers.
 */
public class UserInteractor implements InteractionResultProvider {
    private int mcssAnswer;
    private String textAnswer;

    public UserInteractor() {
        this.mcssAnswer = 0;
        this.textAnswer = "";
    }

    /**
     * Sets the answer for a Multiple Choice Single Selection (MCSS) interaction result.
     * 
     * @param newAnswer the int value of the new answer
     */
    public void setMCSSAnswer(int newAnswer) {
        this.mcssAnswer = newAnswer;
    }

    /**
     * Sets the answer for a text input interaction result.
     * 
     * @param newAnswer the String value of the new answer
     */
    public void setTextAnswer(String newAnswer) {
        this.textAnswer = newAnswer;
    }

    @Override
    public boolean getConfirmationInteractionResult(UserInteractionOptions.WindowModality m, String title, String message, String pos, String neg, String cancel) {
        return true;
    }

    @Override
    public void getNotificationInteractionResult(UserInteractionOptions.WindowModality m, String title, String message, String pos, UserInteractionOptions.NotificationType type) {}

    @Override
    public String getTextInputInteractionResult(UserInteractionOptions.WindowModality m, String title, String message, String pos, String cancel, UserInteractionOptions.InputValidator v) {
        return this.textAnswer;
    }

    @Override
    public int getMultipleChoiceSingleSelectionInteractionResult(UserInteractionOptions.WindowModality m, String title, String message, String pos, String cancel, Iterable<String> choices) {
        return this.mcssAnswer;
    }

    @Override
    public Iterable<Integer> getMultipleChoiceMultipleSelectionInteractionResult(UserInteractionOptions.WindowModality m, String title, String message, String pos, String cancel, Iterable<String> choices) {
        return List.of(0);
    }
}
