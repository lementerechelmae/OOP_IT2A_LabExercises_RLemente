import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform; // FIX: Added missing import for AffineTransform
import java.util.*;
import java.util.List;
import javax.swing.*;

public class ArithmeticGame extends JFrame implements ActionListener {

    // --- Card Identifiers ---
    private static final String WELCOME_CARD = "WELCOME";
    private static final String MODE_CARD = "MODE_SELECTION";
    private static final String GAME_CARD = "GAME";
    private static final String RESULTS_CARD = "RESULTS";

    // --- Colors & Fonts (Balanced Palette) ---
    private static final Color PRIMARY_BLUE = new Color(79, 114, 205); // Moderate Blue
    private static final Color PRIMARY_ORANGE = new Color(255, 128, 0); 
    private static final Color DARK_TEXT = new Color(20, 30, 40);
    private static final Color LIGHT_BACKGROUND = Color.WHITE; // Use pure white for core panels
    private static final Color PALE_BLUE = new Color(220, 235, 255); // Pale background for gradients
    private static final Color SUCCESS_GREEN = new Color(46, 125, 50); // Deep Green
    private static final Color LIME_GREEN = new Color(139, 195, 74); 
    private static final Color WARNING_YELLOW = new Color(255, 193, 7);
    private static final Color DANGER_RED = new Color(211, 47, 47); // Deeper Red
    private static final Color ACCENT_TEAL = new Color(0, 131, 143); // Darker Teal for timer/accents
    private static final Color PURPLE_ACCENT = new Color(94, 53, 177); // Deep Purple for buttons

    private final Font TITLE_FONT = new Font("Impact", Font.BOLD, 75); 
    private final Font HEADER_FONT = new Font("Verdana", Font.BOLD, 32); 
    private final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 20); 
    private final Font PROBLEM_FONT = new Font("Monospaced", Font.BOLD, 65);
    private final Font SYMBOL_FONT = new Font("Arial", Font.BOLD, 40); // Font for background symbols

    // --- Core UI Components ---
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private JLabel problemLabel, scoreLabel, feedbackLabel, resultsMessageLabel, timerLabel;
    private JTextField answerField, itemCountField;
    private JPanel digitContainerPanel;
    private List<JButton> digitButtons;
    private JProgressBar progressBar;
    private JButton hintButton; 

    // --- Game State ---
    private int score = 0, totalItems, currentProblemIndex, correctAnswer, difficultyMax;
    private String operator;
    private int hintsRemaining = 0; 
    private final Random random = new Random();
    private final List<Integer> correctAnswersList = new ArrayList<>();
    private final List<Integer> userAnswersList = new ArrayList<>();
    
    private javax.swing.Timer countdownTimer; 
    private int timeLeft;
    private int streak = 0;

    // --- Custom Gradient Panel ---
    class GradientPanel extends JPanel {
        protected final Color colorStart, colorEnd;
        protected final boolean isRadial;
        
        public GradientPanel(Color start, Color end, boolean radial) { 
            this.colorStart = start; 
            this.colorEnd = end;
            this.isRadial = radial;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();
            
            if (isRadial) {
                // Radial gradient 
                g2d.setPaint(new RadialGradientPaint(
                    w / 2f, h / 2f, 
                    (float) Math.max(w, h) / 2f, 
                    new float[]{0.0f, 1.0f}, 
                    new Color[]{colorStart, colorEnd} 
                ));
            } else {
                // Linear gradient
                g2d.setPaint(new GradientPaint(0, 0, colorStart, 0, h, colorEnd));
            }
            g2d.fillRect(0, 0, w, h);
        }
    }
    
    /** 🎨 Panel with an arithmetic symbol background overlay */
    class SymbolBackgroundPanel extends GradientPanel {
        private final List<Symbol> symbols = new ArrayList<>();
        private final String[] symbolSet = {"+", "-", "×", "÷"}; 
        private final Random rand = new Random();
        private static final int NUM_SYMBOLS = 25; // How many symbols to draw

        public SymbolBackgroundPanel(Color start, Color end, boolean radial) {
            super(start, end, radial);
            setOpaque(false); // Crucial for the background to show through
            generateSymbols();
        }
        
        // Helper class to store symbol properties
        private class Symbol {
            String text;
            int x, y;
            Color color;
            float rotation;
        }

        private void generateSymbols() {
            symbols.clear();
            for (int i = 0; i < NUM_SYMBOLS; i++) {
                Symbol s = new Symbol();
                s.text = symbolSet[rand.nextInt(symbolSet.length)];
                s.color = new Color(DARK_TEXT.getRed(), DARK_TEXT.getGreen(), DARK_TEXT.getBlue(), 30 + rand.nextInt(30)); // Low opacity
                s.rotation = (float) (rand.nextGaussian() * Math.PI / 12); // Small random rotation
                symbols.add(s);
            }
        }
        
        @Override
        public void setSize(int w, int h) {
            super.setSize(w, h);
            // Regenerate positions when the size changes
            for(Symbol s : symbols) {
                s.x = rand.nextInt(w);
                s.y = rand.nextInt(h);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Draw the gradient background first
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(SYMBOL_FONT);
            
            int w = getWidth(), h = getHeight();
            
            // Recalculate positions if dimensions are known but positions are not set
            if(symbols.isEmpty() || (w > 0 && h > 0 && symbols.get(0).x == 0 && symbols.get(0).y == 0)) {
                 for(Symbol s : symbols) {
                    s.x = rand.nextInt(w);
                    s.y = rand.nextInt(h);
                }
            }

            for (Symbol s : symbols) {
                g2d.setColor(s.color);
                
                // Save the current transform
                AffineTransform oldTransform = g2d.getTransform();
                
                // Apply translation and rotation
                g2d.translate(s.x, s.y);
                g2d.rotate(s.rotation);
                
                // Draw the text (rotated/translated)
                g2d.drawString(s.text, 0, 0); 
                
                // Restore the original transform
                g2d.setTransform(oldTransform);
            }
        }
    }


    public ArithmeticGame() {
        setTitle("Math Whiz Challenge: Extreme Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 650);
        setMinimumSize(new Dimension(450, 650));
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        add(cardPanel);

        cardPanel.add(createWelcomePanel(), WELCOME_CARD);
        cardPanel.add(createModeSelectionPanel(), MODE_CARD);
        cardPanel.add(createGamePanel(), GAME_CARD);
        cardPanel.add(createResultsPanel(), RESULTS_CARD);

        cardLayout.show(cardPanel, WELCOME_CARD);
        setVisible(true);
    }

    // =================== Styling Helpers ===================
    private JButton styleButton(JButton button, Color bg) {
        button.setFont(BUTTON_FONT);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 3, true),
                BorderFactory.createEmptyBorder(12, 25, 12, 25)
        ));
        return button;
    }

    private JButton styleDigitButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Monospaced", Font.BOLD, 30));
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(65, 65)); 
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2, true),
                BorderFactory.createLineBorder(bg.brighter(), 3, true)
        ));
        button.setActionCommand("DIGIT_" + text);
        button.addActionListener(this);
        return button;
    }

    // =================== Screens ===================

    /** 🚀 Welcome Panel Design - Muted Gradient + Symbols */
    private JPanel createWelcomePanel() {
        // Uses SymbolBackgroundPanel for the theme
        JPanel panel = new SymbolBackgroundPanel(PALE_BLUE, PRIMARY_BLUE.brighter(), false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(100, 50, 50, 50));
        
        // --- Title Area ---
        JLabel title = new JLabel("<html><center>MATH<br><span style='font-size: 0.7em; font-family: Verdana;'>WHIZ CHALLENGE</span></center></html>", SwingConstants.CENTER);
        title.setFont(TITLE_FONT); 
        title.setForeground(DARK_TEXT); // Darker text for high contrast
        
        JPanel titleWrapper = new JPanel(new GridBagLayout());
        titleWrapper.setOpaque(false); // Make transparent to see the symbols
        titleWrapper.add(title);
        titleWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));
        
        panel.add(titleWrapper, BorderLayout.CENTER);

        // --- Button Area ---
        JButton startButton = styleButton(new JButton("START CHALLENGE"), SUCCESS_GREEN.darker());
        startButton.setFont(new Font("Arial", Font.BOLD, 28));
        startButton.addActionListener(e -> cardLayout.show(cardPanel, MODE_CARD));

        JPanel buttonPanel = new JPanel(); 
        buttonPanel.setOpaque(false); // Make transparent to see the symbols
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));
        buttonPanel.add(startButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /** 🎯 Mode Selection Panel Design - Clean Background + Symbols */
    private JPanel createModeSelectionPanel() {
        // Uses SymbolBackgroundPanel for the theme
        SymbolBackgroundPanel panel = new SymbolBackgroundPanel(PALE_BLUE, LIGHT_BACKGROUND, false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        JLabel title = new JLabel("Select Game Mode", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT); 
        title.setFont(HEADER_FONT); 
        title.setForeground(PURPLE_ACCENT.darker());
        panel.add(title); 
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Item Count Input
        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        itemPanel.setOpaque(false); // Make transparent
        JLabel itemLabel = new JLabel("Number of Problems (3-30):");
        itemLabel.setFont(new Font("Arial", Font.BOLD, 17));
        itemPanel.add(itemLabel);
        itemCountField = new JTextField("10", 3); 
        itemCountField.setFont(new Font("Arial", Font.PLAIN, 18));
        itemCountField.setMaximumSize(new Dimension(60, 35));
        itemCountField.setBorder(BorderFactory.createLineBorder(ACCENT_TEAL, 3, true));
        itemPanel.add(itemCountField); 
        panel.add(itemPanel); 
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        String[] difficulties = {"EASY (0-10, + / -)", "MEDIUM (0-50, All Ops)", "HARD (0-100, All Ops)"};
        Color[] difficultyColors = {SUCCESS_GREEN, PRIMARY_ORANGE.darker(), DANGER_RED};
        int[] maxValues = {10, 50, 100};

        // Difficulty Buttons with specific colors
        for (int i = 0; i < difficulties.length; i++) {
            JButton button = styleButton(new JButton(difficulties[i]), difficultyColors[i]);
            button.setAlignmentX(Component.CENTER_ALIGNMENT); 
            button.setMaximumSize(new Dimension(350, 55));
            final int currentMax = maxValues[i];
            
            button.addActionListener(e -> startGame(currentMax));
            
            panel.add(button); 
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
        }

        JButton quitButton = styleButton(new JButton("QUIT GAME"), DARK_TEXT);
        quitButton.setAlignmentX(Component.CENTER_ALIGNMENT); 
        quitButton.setMaximumSize(new Dimension(250, 45));
        quitButton.addActionListener(e -> System.exit(0)); 
        panel.add(Box.createRigidArea(new Dimension(0, 40)));
        panel.add(quitButton);
        return panel;
    }

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_BACKGROUND); // Clean White Background

        // Header: Score + Timer + Cancel 
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LIGHT_BACKGROUND); 
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, PRIMARY_BLUE));
        
        JButton cancel = new JButton("X CANCEL"); 
        cancel.setFont(new Font("Arial", Font.BOLD, 16)); 
        cancel.setForeground(DANGER_RED.darker());
        cancel.setBackground(Color.WHITE); 
        cancel.setFocusPainted(false); 
        cancel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DANGER_RED, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        cancel.addActionListener(e -> {
            if (countdownTimer != null) countdownTimer.stop();
            cardLayout.show(cardPanel, MODE_CARD);
        });
        header.add(cancel, BorderLayout.WEST);

        scoreLabel = new JLabel("Item 1 / ?", SwingConstants.CENTER); 
        scoreLabel.setFont(new Font("Verdana", Font.BOLD, 28)); 
        scoreLabel.setForeground(DARK_TEXT);
        header.add(scoreLabel, BorderLayout.CENTER);

        timerLabel = new JLabel("Time: 0s", SwingConstants.RIGHT); 
        timerLabel.setFont(new Font("Verdana", Font.BOLD, 20)); 
        timerLabel.setForeground(ACCENT_TEAL); // Darker Teal for readability
        header.add(timerLabel, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        // Center: Problem + Answer
        JPanel center = new JPanel(); 
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(LIGHT_BACKGROUND); 
        center.setBorder(BorderFactory.createEmptyBorder(30, 10, 10, 10));

        problemLabel = new JLabel("Problem"); 
        problemLabel.setFont(PROBLEM_FONT); 
        problemLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(problemLabel); 
        center.add(Box.createRigidArea(new Dimension(0, 25)));

        answerField = new JTextField(4); 
        answerField.setFont(PROBLEM_FONT); 
        answerField.setEditable(false); 
        answerField.setHorizontalAlignment(JTextField.CENTER);
        answerField.setMaximumSize(new Dimension(200, 80)); 
        answerField.setAlignmentX(Component.CENTER_ALIGNMENT);
        answerField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_ORANGE, 8, true), 
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        center.add(answerField);

        // Progress Bar
        progressBar = new JProgressBar(0, totalItems); 
        progressBar.setStringPainted(true); 
        progressBar.setPreferredSize(new Dimension(300, 25));
        progressBar.setForeground(PRIMARY_BLUE);
        progressBar.setBackground(PALE_BLUE);
        progressBar.setBorder(BorderFactory.createLineBorder(DARK_TEXT, 1, true));
        center.add(Box.createRigidArea(new Dimension(0, 25))); 
        center.add(progressBar);

        panel.add(center, BorderLayout.CENTER);

        // Footer: Feedback + Digit Tiles + Controls
        JPanel footer = new JPanel(new BorderLayout()); 
        footer.setBackground(LIGHT_BACKGROUND);
        
        feedbackLabel = new JLabel("Click the tiles to build the answer.", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Verdana", Font.ITALIC, 18)); 
        feedbackLabel.setBorder(BorderFactory.createEmptyBorder(15,0,15,0)); 
        feedbackLabel.setForeground(DARK_TEXT.darker());
        footer.add(feedbackLabel, BorderLayout.NORTH);

        // Digit Container - Light Blue Panel
        digitContainerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10)); 
        digitContainerPanel.setBackground(PALE_BLUE.brighter()); 
        digitContainerPanel.setBorder(BorderFactory.createEmptyBorder(20,15,20,15));
        footer.add(digitContainerPanel, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel(); 
        footer.add(controlPanel, BorderLayout.SOUTH);

        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15)); 
        control.setBackground(LIGHT_BACKGROUND);
        
        JButton reset = styleButton(new JButton("RESET"), WARNING_YELLOW.darker()); 
        reset.setActionCommand("RESET"); 
        reset.addActionListener(this);
        
        JButton submit = styleButton(new JButton("SUBMIT"), SUCCESS_GREEN); 
        submit.setActionCommand("ENT"); 
        submit.addActionListener(this);
        
        // Hint button setup - Deep Purple
        hintButton = styleButton(new JButton("HINT (3)"), PURPLE_ACCENT); 
        hintButton.setActionCommand("HINT"); 
        hintButton.addActionListener(this);
        
        JButton shuffle = styleButton(new JButton("SHUFFLE"), ACCENT_TEAL); 
        shuffle.setActionCommand("SHUFFLE"); 
        shuffle.addActionListener(this);

        control.add(reset); 
        control.add(submit); 
        control.add(hintButton); 
        control.add(shuffle);
        return control;
    }

    /** 🏆 Results Panel Design with cleaner formatting */
    private JPanel createResultsPanel() {
        // Radial Gradient: White center fading to Pale Blue
        JPanel panel = new GradientPanel(Color.WHITE, PALE_BLUE, true); 
        panel.setLayout(new BorderLayout()); 
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        resultsMessageLabel = new JLabel("", SwingConstants.CENTER); 
        resultsMessageLabel.setVerticalAlignment(SwingConstants.TOP);
        panel.add(resultsMessageLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setOpaque(false);
        
        JButton playAgain = styleButton(new JButton("PLAY AGAIN"), SUCCESS_GREEN);
        playAgain.addActionListener(e -> cardLayout.show(cardPanel, MODE_CARD));
        
        JButton exitGame = styleButton(new JButton("EXIT GAME"), DANGER_RED);
        exitGame.addActionListener(e -> System.exit(0));

        btnPanel.add(playAgain);
        btnPanel.add(exitGame); 
        
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // =================== Game Logic ===================
    private void startGame(int max) {
        try {
            int items = Integer.parseInt(itemCountField.getText());
            totalItems = Math.min(30, Math.max(3, items));
        } catch (NumberFormatException ex) { totalItems = 10; }
        
        difficultyMax = max; 
        score = 0; 
        currentProblemIndex = 0; 
        streak = 0;
        hintsRemaining = 3; 
        
        hintButton.setText("HINT (" + hintsRemaining + ")");
        hintButton.setEnabled(true);

        correctAnswersList.clear(); 
        userAnswersList.clear();
        progressBar.setMaximum(totalItems);
        
        generateProblem(); 
        cardLayout.show(cardPanel, GAME_CARD); 
        startTimer();
    }

    private void generateProblem() {
        scoreLabel.setText(String.format("Item %d / %d", currentProblemIndex + 1, totalItems));
        progressBar.setValue(currentProblemIndex);
        answerField.setText(""); 
        feedbackLabel.setText("Click the tiles to build the answer.");

        int max = difficultyMax, min = 1;
        int op1 = random.nextInt(max)+min, op2 = random.nextInt(max)+min;
        
        if(max <= 10){ 
            operator = random.nextBoolean() ? "+" : "-"; 
            if(operator.equals("-") && op1 < op2) {
                int t = op1; op1 = op2; op2 = t;
            } 
        } else { 
            String[] ops = {"+", "-", "*", "/"}; 
            operator = ops[random.nextInt(4)]; 
        }

        switch(operator){
            case"+": correctAnswer = op1 + op2; break;
            case"-": correctAnswer = op1 - op2; break;
            case"*": correctAnswer = op1 * op2; break;
            case"/": 
                correctAnswer = op1 / op2; 
                op1 = correctAnswer * op2; 
                break;
        }

        problemLabel.setText(String.format("%d %s %d",op1,operator,op2));

        // Digit Puzzle setup
        List<Integer> requiredDigits = new ArrayList<>();
        for(char c:String.valueOf(Math.abs(correctAnswer)).toCharArray()) requiredDigits.add(Character.getNumericValue(c));
        
        if (correctAnswer != 0 && !requiredDigits.contains(0) && requiredDigits.size() < 4) {
             requiredDigits.add(0);
        }
        
        List<Integer> pool = new ArrayList<>(requiredDigits);
        int distractors = 10 - requiredDigits.size();
        for(int i = 0; i < distractors; i++) {
            int d; 
            do {
                d = random.nextInt(10);
            } while(pool.contains(d)); 
            pool.add(d);
        }
        
        Collections.shuffle(pool); 
        setupDigitButtons(pool);
    }

    private void setupDigitButtons(List<Integer> pool) {
        digitContainerPanel.removeAll(); 
        digitButtons = new ArrayList<>();
        for(int d: pool){ 
            // Use PURPLE_ACCENT for buttons
            JButton b = styleDigitButton(""+d, PURPLE_ACCENT); 
            digitButtons.add(b); 
            digitContainerPanel.add(b); 
        }
        digitContainerPanel.revalidate(); 
        digitContainerPanel.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if(cmd.startsWith("DIGIT_")){
            String digit = cmd.substring(6);
            if(answerField.getText().length() < 4){ 
                answerField.setText(answerField.getText() + digit); 
                ((JButton)e.getSource()).setEnabled(false); 
                feedbackLabel.setText("Keep building the number."); 
            }
            else feedbackLabel.setText("Answer is 4 digits max. Press SUBMIT or RESET.");
        } else if(cmd.equals("RESET")){
            answerField.setText(""); 
            for(JButton b:digitButtons) b.setEnabled(true); 
            feedbackLabel.setText("Puzzle tiles have been reset.");
        } else if(cmd.equals("ENT")) submitAnswer();
        else if(cmd.equals("HINT")) giveHint();
        else if(cmd.equals("SHUFFLE")) shuffleTiles();
    }

    private void submitAnswer(){
        String input = answerField.getText(); 
        if(input.isEmpty()){ feedbackLabel.setText("Place digits to form an answer."); return; }
        
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        
        try{
            int user = Integer.parseInt(input); 
            userAnswersList.add(user); 
            correctAnswersList.add(correctAnswer);
            
            if(user == correctAnswer){ 
                int points = (streak > 1 ? streak : 1);
                score += points; 
                streak++; 
                feedbackLabel.setText("✅ Correct! +" + points + " points!"); 
                flashBackground(LIME_GREEN.brighter()); 
            } else { 
                streak = 0; 
                feedbackLabel.setText("❌ Incorrect. Correct Answer: " + correctAnswer); 
                flashBackground(DANGER_RED.brighter()); 
            }
            disableDigitButtons();
            
            javax.swing.Timer t = new javax.swing.Timer(1500, e -> {
                ((javax.swing.Timer)e.getSource()).stop(); 
                nextOrEnd();
            }); 
            t.setRepeats(false); 
            t.start();
        } catch(NumberFormatException ex){ 
            feedbackLabel.setText("Error reading input. Press RESET and try again."); 
        }
    }

    private void giveHint(){
        if (hintsRemaining <= 0) {
            feedbackLabel.setText("🚫 No hints remaining! You're on your own, Whiz!");
            hintButton.setEnabled(false);
            return;
        }

        String ans = String.valueOf(Math.abs(correctAnswer));
        boolean hintApplied = false;

        for(int i = 0; i < ans.length(); i++){
            if(answerField.getText().length() <= i){
                char nextDigitChar = ans.charAt(i);
                answerField.setText(answerField.getText() + nextDigitChar);
                
                for(JButton b : digitButtons){ 
                    if(b.getText().equals("" + nextDigitChar) && b.isEnabled()){
                        b.setEnabled(false); 
                        hintApplied = true;
                        break;
                    }
                }
                break;
            }
        }
        
        if (hintApplied) {
            hintsRemaining--;
            hintButton.setText("HINT (" + hintsRemaining + ")");
            feedbackLabel.setText("💡 Hint used! " + hintsRemaining + " remaining.");
            if (hintsRemaining == 0) {
                hintButton.setEnabled(false);
            }
        } else {
             feedbackLabel.setText("❗ Hint could not be applied.");
        }
    }

    private void shuffleTiles(){
        List<String> currentDigits = new ArrayList<>();
        for (JButton b : digitButtons) {
            if (b.isEnabled()) {
                currentDigits.add(b.getText());
            }
        }

        Collections.shuffle(currentDigits); 

        List<JButton> newDigitButtons = new ArrayList<>();
        
        for (String digit : currentDigits) {
            JButton b = digitButtons.stream()
                                    .filter(btn -> btn.getText().equals(digit) && btn.isEnabled())
                                    .findFirst()
                                    .orElse(null);
            if (b != null) {
                newDigitButtons.add(b);
            }
        }

        for (JButton b : digitButtons) {
            if (!b.isEnabled() && !newDigitButtons.contains(b)) {
                newDigitButtons.add(b);
            }
        }
        
        digitButtons = newDigitButtons;
        digitContainerPanel.removeAll();
        for(JButton b: digitButtons) digitContainerPanel.add(b);
        digitContainerPanel.revalidate(); 
        digitContainerPanel.repaint();
        feedbackLabel.setText("🔀 Tiles shuffled!");
    }

    private void disableDigitButtons(){ for(JButton b:digitButtons) b.setEnabled(false); }

    private void flashBackground(Color c){
        Color original = digitContainerPanel.getBackground(); 
        digitContainerPanel.setBackground(c);
        
        javax.swing.Timer t = new javax.swing.Timer(300, e -> {
            ((javax.swing.Timer)e.getSource()).stop(); 
            digitContainerPanel.setBackground(original);
        }); 
        t.setRepeats(false); 
        t.start();
    }

    private void nextOrEnd(){
        currentProblemIndex++; 
        if(currentProblemIndex >= totalItems) endGame(); 
        else { generateProblem(); startTimer(); }
    }

    private void startTimer() {
        timeLeft = 15; 
        timerLabel.setText("Time: " + timeLeft + "s");

        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }

        countdownTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft + "s");
            // Flash red on low time
            if (timeLeft <= 5) {
                timerLabel.setForeground(DANGER_RED);
            } else {
                timerLabel.setForeground(ACCENT_TEAL);
            }

            if (timeLeft <= 0) {
                countdownTimer.stop();
                
                userAnswersList.add(0); 
                correctAnswersList.add(correctAnswer);

                feedbackLabel.setText("⏳ Time's up! The correct answer was: " + correctAnswer);
                disableDigitButtons();
                
                javax.swing.Timer delay = new javax.swing.Timer(1500, ev -> {
                    ((javax.swing.Timer) ev.getSource()).stop();
                    nextOrEnd();
                });
                delay.setRepeats(false);
                delay.start();
            }
        });
        countdownTimer.start();
    }

    private void endGame() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }

        int correctCount = 0;
        StringBuilder detailHtml = new StringBuilder();
        
        detailHtml.append("<div style='text-align: left; font-size: 14px; color: #333; line-height: 1.6;'>");

        for (int i = 0; i < totalItems; i++) {
            
            int userA = userAnswersList.get(i);
            int correctA = correctAnswersList.get(i);
            
            boolean isCorrect = (userA == correctA);
            
            String color = DANGER_RED.toString().replaceAll("java.awt.Color", "#").substring(1); 
            String status = "";

            if (!isCorrect && userA == 0) {
                 status = "Timed Out (Ans: " + correctA + ")";
                 color = WARNING_YELLOW.darker().toString().replaceAll("java.awt.Color", "#").substring(1); 
            } else if (isCorrect) {
                 status = "Correct";
                 correctCount++;
                 color = SUCCESS_GREEN.toString().replaceAll("java.awt.Color", "#").substring(1);
            } else {
                 status = "Incorrect (Ans: " + correctA + ")";
                 color = DANGER_RED.toString().replaceAll("java.awt.Color", "#").substring(1);
            }

            String displayUserA = (userA == 0 && !isCorrect && correctA != 0) ? "-" : String.valueOf(userA);

            // Cleaned up result display
            detailHtml.append(String.format(
                "<span style='color: #%s;'>&#x2713;</span> Item %d: %s (%s)<br>",
                color, i + 1, displayUserA, status 
            ));
        }
        detailHtml.append("</div>");

        String finalScoreColor = (correctCount >= totalItems / 2.0) ? SUCCESS_GREEN.toString().replaceAll("java.awt.Color", "#").substring(1) : DANGER_RED.toString().replaceAll("java.awt.Color", "#").substring(1);

        String htmlMessage = String.format(
            "<html><p align='center' style='font-weight: bold; font-size: 1.5em; color: #111;'>🎉 CHALLENGE COMPLETE!</p>" +
            "<p align='center' style='font-weight: bold;'>Total Points: %d</p>" +
            "<p align='center'>Final Score: <span style='font-size: 2.2em; color: #%s;'>%d / %d</span></p>" +
            "<br><div style='text-align: center; max-width: 350px; margin: auto; padding: 15px; border: 4px solid #%s; background-color: #f7f7f7; border-radius: 20px;'>%s</div></html>",
            score,
            finalScoreColor,
            correctCount, totalItems,
            finalScoreColor,
            detailHtml.toString()
        );

        resultsMessageLabel.setText(htmlMessage);
        cardLayout.show(cardPanel, RESULTS_CARD);
    }

    /** Main method to run the game */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ArithmeticGame::new);
    }

}