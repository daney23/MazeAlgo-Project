package io.github.daney23.mazealgo.view;

import io.github.daney23.mazealgo.model.MazeModel;
import io.github.daney23.mazealgo.viewmodel.MazeViewModel;

/**
 * Controller for {@code MazeView.fxml}. Owns the ViewModel and wires it
 * to FXML-injected nodes. Phase 3 will fill this in with the canvas
 * displayer, key bindings, and zoom support.
 */
public class MazeViewController {

    @SuppressWarnings("unused")
    private final MazeViewModel viewModel = new MazeViewModel(new MazeModel());
}
