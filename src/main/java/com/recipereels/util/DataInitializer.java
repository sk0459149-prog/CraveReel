package com.recipereels.util;

import com.recipereels.entity.*;
import com.recipereels.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeStepRepository stepRepository;
    private final RecipeReelRepository reelRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final RecipeLikeRepository likeRepository;
    private final SystemSettingRepository settingRepository;
    private final RecipeViewHistoryRepository viewHistoryRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           RecipeRepository recipeRepository,
                           RecipeIngredientRepository ingredientRepository,
                           RecipeStepRepository stepRepository,
                           RecipeReelRepository reelRepository,
                           RatingRepository ratingRepository,
                           CommentRepository commentRepository,
                           RecipeLikeRepository likeRepository,
                           SystemSettingRepository settingRepository,
                           RecipeViewHistoryRepository viewHistoryRepository,
                           SavedRecipeRepository savedRecipeRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.stepRepository = stepRepository;
        this.reelRepository = reelRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.settingRepository = settingRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.savedRecipeRepository = savedRecipeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Clean duplicate catalog rows created by earlier seed versions before
        // repairing media or adding any missing recipes.
        deduplicateCatalogRecipes();

        if (userRepository.count() > 0) {
            // Preserve existing user-created data, but add any new catalog recipes
            // that are missing from an already-initialized database.
            seedAdditionalIndianRecipes();
            return;
        }

        // 1. Seed System Settings
        initSettings();

        // 2. Seed Users
        User admin = new User("Platform Admin", "admin@recipereels.com", passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN);
        admin.setAvatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150");
        admin.setBio("Chief culinary platform administrator.");
        userRepository.save(admin);

        User chefRahul = new User("Chef Rahul Sharma", "chef.rahul@recipereels.com", passwordEncoder.encode("Chef@123"), Role.ROLE_CONTRIBUTOR);
        chefRahul.setAvatarUrl("https://images.unsplash.com/photo-1577219491135-ce391730fb2c?w=150");
        chefRahul.setBio("Executive Chef passionate about North Indian delicacies and artisan fusion cuisine.");
        chefRahul.setDietaryPreferences("Vegetarian, Organic");
        userRepository.save(chefRahul);

        User chefPriya = new User("Chef Priya Patel", "chef.priya@recipereels.com", passwordEncoder.encode("Chef@123"), Role.ROLE_CONTRIBUTOR);
        chefPriya.setAvatarUrl("https://images.unsplash.com/photo-1583394293214-28ded15ee548?w=150");
        chefPriya.setBio("Pastry artist and Italian pasta specialist. Quick reels for everyday cooks.");
        chefPriya.setDietaryPreferences("Mediterranean, Baking");
        userRepository.save(chefPriya);

        User userAlex = new User("Alex Johnson", "alex@example.com", passwordEncoder.encode("User@123"), Role.ROLE_USER);
        userAlex.setAvatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
        userAlex.setBio("Food enthusiast and weekend home cook.");
        userAlex.setDietaryPreferences("Non-Vegetarian, Quick Dinners");
        userRepository.save(userAlex);

        User userSarah = new User("Sarah Williams", "sarah@example.com", passwordEncoder.encode("User@123"), Role.ROLE_USER);
        userSarah.setAvatarUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150");
        userSarah.setBio("Fitness lover obsessed with healthy smoothie bowls and salads.");
        userSarah.setDietaryPreferences("Vegetarian, High Protein");
        userRepository.save(userSarah);

        User userDavid = new User("David Chen", "david@example.com", passwordEncoder.encode("User@123"), Role.ROLE_USER);
        userDavid.setAvatarUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150");
        userDavid.setBio("Street food explorer and noodle lover.");
        userRepository.save(userDavid);

        User userElena = new User("Elena Rostova", "elena@example.com", passwordEncoder.encode("User@123"), Role.ROLE_USER);
        userElena.setAvatarUrl("https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150");
        userRepository.save(userElena);

        User userMichael = new User("Michael Scott", "michael@example.com", passwordEncoder.encode("User@123"), Role.ROLE_USER);
        userMichael.setAvatarUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150");
        userRepository.save(userMichael);

        // 3. Seed Categories
        Category indian = categoryRepository.save(new Category("Indian", "indian", "Rich aromas and authentic Indian curries", "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=600", "🍛"));
        Category italian = categoryRepository.save(new Category("Italian", "italian", "Handcrafted pastas, woodfired pizzas and gelato", "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=600", "🍝"));
        Category breakfast = categoryRepository.save(new Category("Breakfast", "breakfast", "Energizing morning feasts and smoothie bowls", "https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?w=600", "🥞"));
        Category desserts = categoryRepository.save(new Category("Desserts", "desserts", "Decadent cakes, molten puddings and pastries", "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600", "🍰"));
        Category healthy = categoryRepository.save(new Category("Healthy", "healthy", "Nutritious, high-protein and wholesome plates", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600", "🥗"));
        Category fastfood = categoryRepository.save(new Category("Fast Food", "fast-food", "Crispy burgers, loaded fries and wraps", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600", "🍔"));
        Category chinese = categoryRepository.save(new Category("Chinese", "chinese", "Wok-tossed noodles, dumplings and stir-fries", "https://images.unsplash.com/photo-1585032226651-759b368d7246?w=600", "🥡"));

        // Video URLs (Reliable royalty-free sample videos)
        String sampleVideo1 = "https://assets.mixkit.co/videos/preview/mixkit-hands-cutting-a-cucumber-in-a-cutting-board-43528-large.mp4";
        String sampleVideo2 = "https://assets.mixkit.co/videos/preview/mixkit-woman-pouring-hot-sauce-over-food-43527-large.mp4";
        String sampleVideo3 = "https://assets.mixkit.co/videos/preview/mixkit-serving-food-on-a-plate-43530-large.mp4";
        String sampleVideo4 = "https://assets.mixkit.co/videos/preview/mixkit-kneading-dough-on-a-wooden-board-43531-large.mp4";

        // 4. Seed Recipes (10 diverse recipes)

        // Recipe 1: Royal Kadhai Paneer Masala
        createFullRecipe(
                chefRahul, indian,
                "Royal Kadhai Paneer Masala",
                "Spicy and flavorful Royal Kadhai Paneer Masala cooked with paneer, onions, tomatoes, green chilli and aromatic Indian spices.",
                15, 25, 4, Difficulty.EASY, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Kadai%20Paneer.JPG?width=1200",
                "https://www.youtube.com/watch?v=gBnHovVnNwo", 60,
                List.of(
                        new String[]{"Paneer", "250", "g"},
                        new String[]{"Onion", "2", "medium"},
                        new String[]{"Tomato", "3", "medium"},
                        new String[]{"Ginger Garlic Paste", "1", "tbsp"},
                        new String[]{"Green Chilli", "2", "pieces"},
                        new String[]{"Turmeric Powder", "1/2", "tsp"},
                        new String[]{"Red Chilli Powder", "1", "tsp"},
                        new String[]{"Coriander Powder", "1", "tsp"},
                        new String[]{"Garam Masala", "1/2", "tsp"},
                        new String[]{"Cooking Oil", "2", "tbsp"},
                        new String[]{"Salt", "1", "tsp"},
                        new String[]{"Fresh Coriander", "2", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Prepare the Masala", "Heat oil in a pan. Add chopped onion and cook until it becomes soft and lightly golden.", 5, "Keep the flame on medium and stir occasionally."),
                        new StepData(2, "Add Ginger, Garlic & Chilli", "Add ginger garlic paste and green chilli. Cook until the raw smell goes away.", 2, "Do not burn the ginger-garlic paste."),
                        new StepData(3, "Cook the Tomato Masala", "Add chopped tomatoes, turmeric powder, red chilli powder, coriander powder and salt. Cook until the tomatoes become soft and the masala becomes thick.", 8, "Cook until the oil starts separating from the masala."),
                        new StepData(4, "Add Paneer", "Add the paneer pieces to the prepared masala and gently mix until the paneer is coated evenly.", 3, "Mix gently so the paneer pieces do not break."),
                        new StepData(5, "Finish & Serve", "Add garam masala and fresh coriander. Mix gently, cook briefly and serve hot.", 1, "Serve the paneer masala hot.")
                ),
                List.of(userAlex, userSarah),
                List.of(new RatingData(userAlex, 5, "Best paneer recipe on the internet! The reel made it so simple to replicate.")),
                List.of(new CommentData(userSarah, "Can I substitute butter with coconut oil for vegan?"))
        );

        // Recipe 2: Creamy Garlic Alfredo Pasta
        createFullRecipe(
                chefPriya, italian,
                "Silky Garlic Alfredo Fettuccine",
                "Classic Italian fettuccine tossed in a rich emulsified parmesan, heavy cream and toasted garlic sauce with cracked black pepper.",
                10, 15, 2, Difficulty.EASY, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Fettuccine%20Alfredo.jpg?width=1200",
                "https://www.youtube.com/watch?v=vqOKjMzoGn4", 40,
                List.of(
                        new String[]{"Fettuccine Pasta", "200", "g"},
                        new String[]{"Butter", "50", "g"},
                        new String[]{"Heavy Whipping Cream", "200", "ml"},
                        new String[]{"Garlic Cloves (minced)", "3", "cloves"},
                        new String[]{"Parmigiano Reggiano (grated)", "80", "g"},
                        new String[]{"Fresh Parsley", "2", "tbsp"},
                        new String[]{"Cracked Black Pepper", "1", "tsp"}
                ),
                List.of(
                        new StepData(1, "Boil the Pasta", "Bring a large pot of heavily salted water to a rolling boil. Cook fettuccine al dente according to package instructions.", 8, "Reserve half a cup of pasta cooking water before draining."),
                        new StepData(2, "Infuse Garlic & Butter", "In a large skillet, melt butter over medium heat. Sauté minced garlic for 60 seconds until fragrant.", 2, "Keep heat gentle to prevent garlic from turning bitter."),
                        new StepData(3, "Simmer Cream", "Pour in heavy cream and bring to a gentle simmer for 3 minutes until slightly reduced.", 3, "Whisk continuously."),
                        new StepData(4, "Emulsify Sauce", "Turn heat to low. Whisk in grated parmesan cheese until velvety and completely melted.", 2, "Turn off heat before cheese goes in so it doesn't clump."),
                        new StepData(5, "Toss Pasta", "Toss drained fettuccine into the sauce. Add a splash of pasta water if needed to coat every strand. Garnish with chopped parsley and black pepper.", 1, "Twirl onto warm bowls.")
                ),
                List.of(userAlex, userDavid, userElena),
                List.of(new RatingData(userDavid, 5, "Incredible texture! My kids ate every single bite."), new RatingData(userElena, 4, "Super quick dinner. Make sure to use real parmesan.")),
                List.of(new CommentData(userDavid, "The Cooking Mode timer kept me from overcooking the pasta!"))
        );

        // Recipe 3: Crispy Gourmet Smash Burger
        createFullRecipe(
                chefRahul, fastfood,
                "Crispy Double Cheese Smash Burger",
                "Crispy lacy-edged smashed beef/chicken patties layered with melted cheddar, caramelized onions, and tangy homemade secret sauce on toasted brioche.",
                15, 10, 2, Difficulty.MEDIUM, false, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Cheeseburger.jpg?width=1200",
                "https://www.youtube.com/watch?v=B8gyW3N3eGY", 50,
                List.of(
                        new String[]{"Ground Meat Patty Ball", "300", "g"},
                        new String[]{"Brioche Buns", "2", "pairs"},
                        new String[]{"American Cheese Slices", "4", "slices"},
                        new String[]{"Yellow Onion (thinly sliced)", "1", "piece"},
                        new String[]{"Mayonnaise", "3", "tbsp"},
                        new String[]{"Dijon Mustard", "1", "tbsp"},
                        new String[]{"Dill Pickles", "6", "slices"}
                ),
                List.of(
                        new StepData(1, "Prep Burger Sauce", "Mix mayonnaise, mustard, finely diced pickles, and a dash of paprika in a bowl. Refrigerate.", 5, "Chilled sauce holds together much better."),
                        new StepData(2, "Toast Buns", "Toast brioche buns on a dry cast iron skillet until golden brown. Set aside.", 2, "Toasting creates a barrier against burger juices."),
                        new StepData(3, "Smash Patties", "Heat cast iron skillet smoking hot. Place meat ball, top with onions, and smash paper-thin with a heavy spatula.", 3, "Press firmly for 10 seconds to create the signature crispy crust."),
                        new StepData(4, "Flip & Melt Cheese", "Flip patty once deep caramelized crust forms. Top with cheese and let melt for 60 seconds.", 1, "Steam with a bowl over the patty for instant melt."),
                        new StepData(5, "Assemble", "Spread sauce on bottom bun, stack double cheese patties, top with pickles and top bun. Press gently and enjoy.", 1, "Serve with crinkle fries.")
                ),
                List.of(userAlex, userMichael),
                List.of(new RatingData(userMichael, 5, "Restaurant quality smash burger right at home.")),
                List.of(new CommentData(userAlex, "That smash crust is out of this world!"))
        );

        // Recipe 4: Molten Chocolate Lava Cake
        createFullRecipe(
                chefPriya, desserts,
                "Decadent Molten Chocolate Lava Cake",
                "Warm individual chocolate cakes with an irresistibly oozing molten chocolate core, served with vanilla bean ice cream.",
                15, 12, 2, Difficulty.MEDIUM, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Chocolate%20lava%20cake.jpg?width=1200",
                "https://www.youtube.com/watch?v=RD3ZBBXMn4c", 55,
                List.of(
                        new String[]{"Dark Chocolate (70%)", "100", "g"},
                        new String[]{"Unsalted Butter", "100", "g"},
                        new String[]{"Eggs", "2", "whole"},
                        new String[]{"Egg Yolks", "2", "yolks"},
                        new String[]{"Powdered Sugar", "50", "g"},
                        new String[]{"All-Purpose Flour", "30", "g"},
                        new String[]{"Cocoa Powder", "1", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Melt Chocolate & Butter", "Melt dark chocolate and butter in a heatproof bowl set over simmering water until glossy and smooth.", 5, "Use high quality chocolate for best flavor."),
                        new StepData(2, "Whip Eggs & Sugar", "In a separate bowl, whisk whole eggs, yolks, and powdered sugar until pale and frothy.", 3, "Whisk vigorously for light airy cake walls."),
                        new StepData(3, "Combine & Sift", "Fold melted chocolate mixture into the eggs. Sift in flour and gently fold until just incorporated.", 2, "Do not overmix."),
                        new StepData(4, "Bake to Perfection", "Grease ramekins with butter and dust with cocoa powder. Pour batter and bake at 200°C (400°F) for exactly 11 minutes.", 11, "The edges should be firm while the center remains soft and jiggly."),
                        new StepData(5, "Invert & Serve", "Let rest for 1 minute. Carefully invert onto dessert plates. Dust with powdered sugar and serve with vanilla ice cream.", 1, "Serve immediately while molten.")
                ),
                List.of(userSarah, userElena),
                List.of(new RatingData(userSarah, 5, "Pure heaven! Baked it for date night and it was a hit.")),
                List.of(new CommentData(userElena, "My favorite dessert recipe forever."))
        );

        // Recipe 5: Vibrant Berry Acai Smoothie Bowl
        createFullRecipe(
                chefPriya, healthy,
                "Vibrant Antioxidant Berry Smoothie Bowl",
                "Thick, luscious frozen berry smoothie bowl topped with sliced banana, chia seeds, toasted coconut flakes and golden honey.",
                10, 0, 1, Difficulty.EASY, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Smoothie%20bowl.jpg?width=1200",
                sampleVideo1, 35,
                List.of(
                        new String[]{"Frozen Mixed Berries", "200", "g"},
                        new String[]{"Frozen Banana", "1", "piece"},
                        new String[]{"Greek Yogurt / Plant Milk", "100", "ml"},
                        new String[]{"Chia Seeds", "1", "tbsp"},
                        new String[]{"Granola", "2", "tbsp"},
                        new String[]{"Honey or Maple Syrup", "1", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Blend Thick Smoothie", "Add frozen berries, frozen banana and a splash of milk into a high-speed blender. Blend until thick like soft-serve ice cream.", 3, "Use tamper tool to keep it thick without adding excess liquid."),
                        new StepData(2, "Pour into Bowl", "Spoon the vibrant purple mixture into a chilled ceramic bowl and smooth the surface.", 1, "Chilling the bowl keeps the bowl cold longer."),
                        new StepData(3, "Decorate with Toppings", "Arrange sliced fresh strawberries, bananas, chia seeds, crunchy granola and toasted coconut in neat rows. Drizzle honey on top.", 2, "Eat immediately with a spoon.")
                ),
                List.of(userSarah),
                List.of(new RatingData(userSarah, 5, "My daily breakfast inspiration!")),
                List.of(new CommentData(userAlex, "So refreshing on hot mornings."))
        );

        // Recipe 6: Street Style Hakka Noodles
        createFullRecipe(
                chefRahul, chinese,
                "Wok-Tossed Vegetable Hakka Noodles",
                "Smoky indo-chinese street style noodles tossed with shredded cabbage, crunchy capsicum, garlic, and dark soy sauce on high flame.",
                15, 10, 3, Difficulty.EASY, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Hakka%20Noodles.JPG?width=1200",
                "https://www.youtube.com/watch?v=Sww2FXkTKO4", 45,
                List.of(
                        new String[]{"Hakka Noodles", "200", "g"},
                        new String[]{"Shredded Cabbage", "1", "cup"},
                        new String[]{"Bell Pepper (thin strips)", "1", "piece"},
                        new String[]{"Carrot (julienned)", "1", "piece"},
                        new String[]{"Garlic (finely chopped)", "1", "tbsp"},
                        new String[]{"Dark Soy Sauce", "1.5", "tbsp"},
                        new String[]{"Chili Vinegar", "1", "tsp"},
                        new String[]{"Toasted Sesame Oil", "1", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Boil Noodles", "Boil noodles in salted water for 4 minutes until 90% cooked. Drain and rinse under cold water, toss with 1 tsp oil.", 4, "Cold rinse stops cooking and prevents sticking."),
                        new StepData(2, "Flash Fry Aromatics", "Heat wok smoking hot. Add sesame oil, chopped garlic and sliced onions. Toss for 30 seconds on high flame.", 1, "Wok hei comes from high continuous heat."),
                        new StepData(3, "Toss Vegetables", "Toss in cabbage, carrots, and bell peppers. Stir fry rapidly for 2 minutes to keep them crunchy.", 2, "Never overcook veggies in stir fry."),
                        new StepData(4, "Sauce & Noodles", "Add boiled noodles, soy sauce, green chili sauce, vinegar and black pepper. Toss vigorously with tongs.", 2, "Toss from bottom to top evenly."),
                        new StepData(5, "Serve", "Garnish with finely chopped spring onion greens and serve hot.", 1, "Pair with Manchurian.")
                ),
                List.of(userDavid, userMichael),
                List.of(new RatingData(userDavid, 5, "Tastes exactly like Kolkata street food! Excellent.")),
                List.of(new CommentData(userMichael, "Loved the wok hei technique in the reel."))
        );

        // Recipe 7: Crispy South Indian Masala Dosa
        createFullRecipe(
                chefRahul, indian,
                "Crispy Golden Masala Dosa",
                "Paper-thin fermented rice crepe roasted golden in ghee, stuffed with spiced turmeric potato masala and served with coconut chutney.",
                20, 15, 3, Difficulty.MEDIUM, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Masala%20Dosa.jpg?width=1200",
                "https://www.youtube.com/watch?v=PFG1aeYgi7c", 60,
                List.of(
                        new String[]{"Fermented Dosa Batter", "3", "cups"},
                        new String[]{"Boiled Potatoes (mashed)", "3", "pieces"},
                        new String[]{"Mustard Seeds", "1", "tsp"},
                        new String[]{"Curry Leaves", "10", "leaves"},
                        new String[]{"Green Chilies", "2", "pieces"},
                        new String[]{"Turmeric Powder", "0.5", "tsp"},
                        new String[]{"Desi Ghee", "3", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Make Potato Masala", "Heat oil, crackle mustard seeds, curry leaves, and green chilies. Sauté sliced onions until translucent. Add turmeric and mashed potatoes with salt. Cook 5 mins.", 5, "Mash potatoes roughly with a fork."),
                        new StepData(2, "Heat Tawa", "Heat a cast iron dosa tawa. Splash a few drops of water to check temperature, wipe clean with a cloth.", 1, "Even heat is the secret to non-stick dosa."),
                        new StepData(3, "Spread Batter", "Pour a ladleful of batter in center and spread outward in circular spiral motion into a thin crepe.", 1, "Keep motion steady and light."),
                        new StepData(4, "Roast with Ghee", "Drizzle generous ghee around edges and center. Roast on medium heat until base turns deep golden and crisp.", 3, "Do not flip."),
                        new StepData(5, "Fill & Roll", "Place a spoonful of potato masala in center, fold edges over into a roll or triangle. Serve hot with coconut chutney and sambar.", 1, "Eat fresh while crisp.")
                ),
                List.of(userSarah, userAlex),
                List.of(new RatingData(userAlex, 5, "Crispy perfection. The sound in the reel was so satisfying!")),
                List.of(new CommentData(userSarah, "My family devoured this for Sunday brunch."))
        );

        // Recipe 8: Classic Neapolitan Margherita Pizza
        createFullRecipe(
                chefPriya, italian,
                "Classic Margherita Woodfired Pizza",
                "Authentic Neapolitan style pizza with blistered airy crust, crushed San Marzano tomato sauce, fresh buffalo mozzarella and basil.",
                30, 8, 2, Difficulty.HARD, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Margherita%20Pizza.jpg?width=1200",
                "https://www.youtube.com/watch?v=sv3TXMSv6Lw", 50,
                List.of(
                        new String[]{"Pizza Dough Ball", "250", "g"},
                        new String[]{"San Marzano Crushed Tomatoes", "100", "g"},
                        new String[]{"Fresh Mozzarella", "120", "g"},
                        new String[]{"Fresh Basil Leaves", "8", "leaves"},
                        new String[]{"Extra Virgin Olive Oil", "1", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Stretch Dough", "Gently press dough ball on semolina flour, pushing air into the crust rim (cornicione). Stretch out to 10-12 inches.", 5, "Never use a rolling pin; it destroys the air bubbles."),
                        new StepData(2, "Top Pizza", "Spoon crushed tomatoes evenly leaving the border clear. Tear fresh mozzarella over top.", 2, "Drain mozzarella well to avoid watery pizza."),
                        new StepData(3, "Bake at Max Heat", "Transfer onto a preheated pizza stone at 250°C (or pizza oven at 450°C) and bake 6-8 minutes until blistered.", 7, "Look for leopard spots on the crust."),
                        new StepData(4, "Finish & Slice", "Garnish immediately with fresh basil leaves and a spiral of peppery olive oil. Slice and enjoy.", 1, "Serve right away.")
                ),
                List.of(userDavid, userElena),
                List.of(new RatingData(userElena, 5, "Chewy, airy crust just like Naples!")),
                List.of(new CommentData(userDavid, "What flour do you recommend? (Tipo 00?)"))
        );

        // Recipe 9: Fluffy Japanese Souffle Pancakes
        createFullRecipe(
                chefPriya, breakfast,
                "Fluffy Japanese Souffle Cloud Pancakes",
                "Impossibly tall, jiggly cloud pancakes made with whipped meringue and dusted with powdered sugar and maple butter.",
                20, 15, 2, Difficulty.MEDIUM, true, RecipeStatus.APPROVED,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Fluffy%20Japanese%20Pancake%20-%20Shibuya%20%2841950827231%29.jpg?width=1200",
                "https://www.youtube.com/watch?v=i3SktcOL4ko", 55,
                List.of(
                        new String[]{"Egg Yolks", "2", "yolks"},
                        new String[]{"Egg Whites", "3", "whites"},
                        new String[]{"Milk", "2", "tbsp"},
                        new String[]{"Flour", "30", "g"},
                        new String[]{"Sugar", "30", "g"},
                        new String[]{"Maple Syrup", "2", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Whisk Yolk Base", "Whisk yolks with milk and vanilla until frothy. Sift in flour and whisk into smooth paste.", 3, "No lumps should remain."),
                        new StepData(2, "Whip Meringue", "In a clean bowl, beat egg whites while gradually adding sugar until stiff glossy peaks form.", 5, "Meringue must hold its peak upside down."),
                        new StepData(3, "Fold Gently", "Gently fold 1/3 of meringue into yolk mix, then fold remaining meringue without deflating air.", 2, "Use light circular folding technique."),
                        new StepData(4, "Steam Cook Pancakes", "Heat nonstick pan on low. Pipe tall scoops of batter. Add a spoonful of water to pan, cover with lid and steam for 5 minutes per side.", 10, "Steam keeps them ultra moist and soaring high."),
                        new StepData(5, "Serve", "Stack jiggly pancakes on plate, dust with powdered sugar, top with butter and maple syrup.", 1, "Serve within minutes before they deflate.")
                ),
                List.of(userSarah, userAlex),
                List.of(new RatingData(userSarah, 5, "They really jiggle! Incredible recipe.")),
                List.of(new CommentData(userAlex, "The Cooking Mode timer is a lifesaver for the 5-min steam step!"))
        );

        // Recipe 10: Hyderabadi Dum Biryani (PENDING Review for Admin Demo)
        createFullRecipe(
                chefRahul, indian,
                "Aromatic Hyderabadi Dum Chicken Biryani",
                "Fragrant basmati rice layered with marinated spiced chicken, fried brown onions (birista), saffron milk and fresh mint, slow-cooked in sealed dum.",
                40, 45, 6, Difficulty.HARD, false, RecipeStatus.PENDING,
                "https://commons.wikimedia.org/wiki/Special:FilePath/Chicken%20Biryani.jpg?width=1200",
                "https://www.youtube.com/watch?v=rtR9T850vfU", 60,
                List.of(
                        new String[]{"Basmati Rice", "500", "g"},
                        new String[]{"Chicken Pieces", "750", "g"},
                        new String[]{"Yogurt", "1", "cup"},
                        new String[]{"Fried Onions (Birista)", "1", "cup"},
                        new String[]{"Biryani Masala", "2", "tbsp"},
                        new String[]{"Saffron strands in warm milk", "4", "tbsp"},
                        new String[]{"Ghee", "3", "tbsp"}
                ),
                List.of(
                        new StepData(1, "Marinate Chicken", "Marinate chicken with yogurt, ginger garlic paste, biryani masala, red chili, mint, and fried onions for at least 1 hour.", 15, "Longer marinade produces tender chicken."),
                        new StepData(2, "Parboil Rice", "Boil whole spices in water. Add soaked rice and cook until 70% done. Drain immediately.", 8, "Grain should feel firm in center."),
                        new StepData(3, "Layer Pot", "In a heavy bottom handi, lay marinated chicken at bottom. Layer hot rice over chicken. Drizzle saffron milk, ghee and mint leaves.", 5, "Seal tightly with foil or dough."),
                        new StepData(4, "Dum Cook", "Cook on high flame for 10 minutes, then place on a hot tawa and cook on lowest flame for 30 minutes.", 35, "Do not open lid until dum completes."),
                        new StepData(5, "Fluff & Serve", "Gently fluff rice from bottom to top revealing layers of white, yellow and orange rice with tender meat. Serve with mirchi ka salan and raita.", 2, "Serve with cold raita.")
                ),
                List.of(),
                List.of(),
                List.of()
        );

        // Add the expanded Indian catalog on both fresh and existing databases.
        seedAdditionalIndianRecipes();
    }

    private void deduplicateCatalogRecipes() {
        List<String> catalogTitles = List.of(
                "Royal Kadhai Paneer Masala",
                "Silky Garlic Alfredo Fettuccine",
                "Crispy Double Cheese Smash Burger",
                "Decadent Molten Chocolate Lava Cake",
                "Vibrant Antioxidant Berry Smoothie Bowl",
                "Wok-Tossed Vegetable Hakka Noodles",
                "Crispy Golden Masala Dosa",
                "Classic Margherita Woodfired Pizza",
                "Fluffy Japanese Souffle Cloud Pancakes",
                "Aromatic Hyderabadi Dum Chicken Biryani",
                "Royal Shahi Paneer",
                "Slow-Cooked Dal Makhani",
                "Punjabi Chole Masala",
                "Creamy Palak Paneer",
                "Mumbai Pav Bhaji",
                "Homestyle Aloo Gobi Masala"
        );

        for (String title : catalogTitles) {
            List<Recipe> matches = recipeRepository.findAllByTitle(title);
            if (matches.size() <= 1) {
                continue;
            }

            // Keep the newest seeded copy; it contains the corrected media.
            Recipe keep = matches.stream()
                    .max(Comparator.comparing(Recipe::getId))
                    .orElse(matches.get(matches.size() - 1));

            for (Recipe recipe : matches) {
                if (!recipe.getId().equals(keep.getId())) {
                    // These two tables reference recipes but are not cascaded from Recipe.
                    // Remove their rows first so MySQL foreign-key constraints allow the
                    // duplicate catalog recipe to be deleted safely.
                    viewHistoryRepository.deleteByRecipe(recipe);
                    savedRecipeRepository.deleteByRecipe(recipe);
                    recipeRepository.delete(recipe);
                }
            }
        }
    }

    private void seedAdditionalIndianRecipes() {
        User userAlex = userRepository.findByEmail("alex@example.com").orElse(null);
        User userSarah = userRepository.findByEmail("sarah@example.com").orElse(null);
        User userDavid = userRepository.findByEmail("david@example.com").orElse(null);
        User userElena = userRepository.findByEmail("elena@example.com").orElse(null);
        User userMichael = userRepository.findByEmail("michael@example.com").orElse(null);
        User chefRahul = userRepository.findByEmail("chef.rahul@recipereels.com").orElse(null);
        Category indian = categoryRepository.findBySlug("indian").orElse(null);
        if (chefRahul == null || indian == null) {
            return;
        }

        // These stock clips are intentionally used as temporary visual reels.
        // Replace them with dish-specific uploaded reels when available.
        // Do not reuse generic stock clips for named dishes. Each expanded Indian recipe below
        // uses a dish-specific recipe video that was verified before being attached.
        // Also repair media on records created by an earlier version of the seed so the
        // corrected catalog is applied without requiring a database reset.
        updateVerifiedMedia("Royal Kadhai Paneer Masala",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Kadai%20Paneer.JPG?width=1200",
                "https://www.youtube.com/watch?v=gBnHovVnNwo", 60);
        updateVerifiedMedia("Royal Shahi Paneer",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Shahi%20Paneer.jpg?width=1200",
                "https://www.youtube.com/watch?v=sKKsePHfFIA", 45);
        updateVerifiedMedia("Slow-Cooked Dal Makhani",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Dal%20Makhani%2001.jpg?width=1200",
                "https://www.youtube.com/watch?v=f1lpCi_70sQ", 45);
        updateVerifiedMedia("Punjabi Chole Masala",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Chana%20masala%20%285727362126%29.jpg?width=1200",
                "https://www.youtube.com/watch?v=aKSbKQOgTKQ", 42);
        updateVerifiedMedia("Creamy Palak Paneer",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Palak%20Paneer.JPG?width=1200",
                "https://www.youtube.com/watch?v=93c0dARFCYw", 40);
        updateVerifiedMedia("Mumbai Pav Bhaji",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Pav%20Bhaji.jpg?width=1200",
                "https://www.youtube.com/watch?v=H7aGRi70wbU", 38);
        updateVerifiedMedia("Homestyle Aloo Gobi Masala",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Aloo%20Gobi%20Masala%20-%20Mohali%202016-08-07%208551.JPG?width=1200",
                "https://www.youtube.com/watch?v=y7RUpvhET08", 35);
        updateVerifiedMedia("Crispy Golden Masala Dosa",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Masala%20Dosa.jpg?width=1200",
                "https://www.youtube.com/watch?v=PFG1aeYgi7c", 60);
        updateVerifiedMedia("Aromatic Hyderabadi Dum Chicken Biryani",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Chicken%20Biryani.jpg?width=1200",
                "https://www.youtube.com/watch?v=rtR9T850vfU", 60);

        if (!recipeRepository.existsByTitle("Royal Shahi Paneer")) {
            createFullRecipe(
                    chefRahul, indian,
                    "Royal Shahi Paneer",
                    "A rich North Indian paneer curry with onion, tomato, cashews and aromatic spices, finished with a silky creamy texture.",
                    15, 25, 4, Difficulty.MEDIUM, true, RecipeStatus.APPROVED,
                    "https://commons.wikimedia.org/wiki/Special:FilePath/Shahi%20Paneer.jpg?width=1200",
                    "https://www.youtube.com/watch?v=sKKsePHfFIA", 45,
                    List.of(
                            new String[]{"Paneer", "250", "g"},
                            new String[]{"Onion", "2", "medium"},
                            new String[]{"Tomatoes", "2", "medium"},
                            new String[]{"Cashew Nuts", "12", "pieces"},
                            new String[]{"Ginger Garlic Paste", "1", "tbsp"},
                            new String[]{"Red Chilli Powder", "1/2", "tsp"},
                            new String[]{"Garam Masala", "1/2", "tsp"},
                            new String[]{"Cream", "2", "tbsp"},
                            new String[]{"Cooking Oil", "2", "tbsp"}
                    ),
                    List.of(
                            new StepData(1, "Cook Onion & Cashew", "Saute onion until soft, add cashews and cook briefly.", 6, "Keep the flame medium."),
                            new StepData(2, "Make the Base", "Add tomatoes and ginger garlic paste. Cook until soft, cool slightly and blend smooth.", 8, "Blend with a splash of water if needed."),
                            new StepData(3, "Cook the Gravy", "Return the puree to the pan and add chilli powder, garam masala and salt.", 7, "Cook until the gravy thickens."),
                            new StepData(4, "Add Paneer", "Add paneer cubes and gently coat them in the gravy.", 3, "Avoid overcooking paneer."),
                            new StepData(5, "Finish", "Stir in cream and garnish with coriander.", 1, "Serve hot with naan or rice.")
                    ),
                    List.of(userAlex), List.of(new RatingData(userAlex, 5, "Rich and comforting paneer curry.")),
                    List.of(new CommentData(userSarah, "Perfect with naan."))
            );
        }

        if (!recipeRepository.existsByTitle("Slow-Cooked Dal Makhani")) {
            createFullRecipe(
                    chefRahul, indian,
                    "Slow-Cooked Dal Makhani",
                    "Creamy black lentils and kidney beans simmered slowly with tomato, ginger, garlic and warming spices.",
                    15, 45, 4, Difficulty.MEDIUM, true, RecipeStatus.APPROVED,
                    "https://commons.wikimedia.org/wiki/Special:FilePath/Dal%20Makhani%2001.jpg?width=1200",
                    "https://www.youtube.com/watch?v=f1lpCi_70sQ", 45,
                    List.of(
                            new String[]{"Whole Black Urad Dal", "1", "cup"},
                            new String[]{"Rajma", "1/4", "cup"},
                            new String[]{"Tomatoes", "3", "medium"},
                            new String[]{"Ginger Garlic Paste", "1", "tbsp"},
                            new String[]{"Butter", "2", "tbsp"},
                            new String[]{"Cream", "2", "tbsp"},
                            new String[]{"Garam Masala", "1/2", "tsp"},
                            new String[]{"Red Chilli Powder", "1/2", "tsp"}
                    ),
                    List.of(
                            new StepData(1, "Soak the Lentils", "Wash urad dal and rajma and soak them for several hours.", 10, "Soaking shortens the cooking time."),
                            new StepData(2, "Pressure Cook", "Cook the soaked lentils with fresh water and salt until completely tender.", 20, "The lentils should mash easily."),
                            new StepData(3, "Build the Masala", "Saute ginger garlic paste and tomato puree with chilli powder.", 8, "Cook until the oil separates."),
                            new StepData(4, "Simmer", "Add cooked dal and simmer gently so the flavors combine.", 15, "Slow simmering develops the signature texture."),
                            new StepData(5, "Finish", "Add butter, cream and garam masala. Garnish with coriander.", 2, "Serve hot with naan or rice.")
                    ),
                    List.of(userSarah), List.of(new RatingData(userSarah, 5, "Creamy and delicious.")),
                    List.of(new CommentData(userAlex, "A weekend favorite."))
            );
        }

        if (!recipeRepository.existsByTitle("Punjabi Chole Masala")) {
            createFullRecipe(
                    chefRahul, indian,
                    "Punjabi Chole Masala",
                    "Bold Punjabi chickpea curry cooked with onion, tomato, ginger, garlic and a fragrant roasted spice blend.",
                    15, 30, 4, Difficulty.EASY, true, RecipeStatus.APPROVED,
                    "https://commons.wikimedia.org/wiki/Special:FilePath/Chana%20masala%20%285727362126%29.jpg?width=1200",
                    "https://www.youtube.com/watch?v=aKSbKQOgTKQ", 42,
                    List.of(
                            new String[]{"Chickpeas", "2", "cups"},
                            new String[]{"Onion", "2", "medium"},
                            new String[]{"Tomatoes", "3", "medium"},
                            new String[]{"Ginger Garlic Paste", "1", "tbsp"},
                            new String[]{"Chole Masala", "2", "tbsp"},
                            new String[]{"Red Chilli Powder", "1/2", "tsp"},
                            new String[]{"Coriander Powder", "1", "tsp"},
                            new String[]{"Amchur Powder", "1/2", "tsp"}
                    ),
                    List.of(
                            new StepData(1, "Prepare Chickpeas", "Cook soaked chickpeas until tender and keep them ready.", 15, "Do not discard all the cooking liquid."),
                            new StepData(2, "Saute Aromatics", "Cook chopped onion with ginger garlic paste until golden.", 7, "Golden onion adds depth."),
                            new StepData(3, "Cook Tomato Masala", "Add tomatoes and dry spices and cook until thick.", 8, "Cook until the oil separates."),
                            new StepData(4, "Add Chickpeas", "Add chickpeas and enough cooking liquid. Simmer until the gravy coats the chickpeas.", 10, "Lightly mash a few chickpeas for body."),
                            new StepData(5, "Finish", "Add amchur and fresh coriander and serve hot.", 2, "Great with bhature or rice.")
                    ),
                    List.of(userDavid), List.of(new RatingData(userDavid, 5, "Authentic Punjabi flavors.")),
                    List.of(new CommentData(userElena, "Looks amazing with bhature."))
            );
        }

        if (!recipeRepository.existsByTitle("Creamy Palak Paneer")) {
            createFullRecipe(
                    chefRahul, indian,
                    "Creamy Palak Paneer",
                    "Vibrant spinach gravy with tender paneer, garlic and aromatic Indian spices.",
                    15, 20, 4, Difficulty.MEDIUM, true, RecipeStatus.APPROVED,
                    "https://commons.wikimedia.org/wiki/Special:FilePath/Palak%20Paneer.JPG?width=1200",
                    "https://www.youtube.com/watch?v=93c0dARFCYw", 40,
                    List.of(
                            new String[]{"Paneer", "250", "g"},
                            new String[]{"Fresh Spinach", "300", "g"},
                            new String[]{"Onion", "1", "large"},
                            new String[]{"Tomato", "1", "medium"},
                            new String[]{"Ginger Garlic Paste", "1", "tbsp"},
                            new String[]{"Green Chilli", "1", "piece"},
                            new String[]{"Garam Masala", "1/2", "tsp"},
                            new String[]{"Cream", "2", "tbsp"}
                    ),
                    List.of(
                            new StepData(1, "Blanch Spinach", "Blanch spinach briefly in hot water and transfer it to cold water.", 2, "This helps retain the green color."),
                            new StepData(2, "Blend", "Blend spinach with green chilli into a smooth puree.", 2, "Do not overblend."),
                            new StepData(3, "Cook Masala", "Saute onion, tomato and ginger garlic paste until soft.", 8, "Cook until the raw smell disappears."),
                            new StepData(4, "Combine", "Add spinach puree and garam masala and simmer gently.", 7, "Avoid prolonged boiling."),
                            new StepData(5, "Add Paneer", "Add paneer and cream and cook briefly before serving.", 3, "Keep paneer soft and tender.")
                    ),
                    List.of(userSarah), List.of(new RatingData(userSarah, 5, "Bright, creamy and easy.")),
                    List.of(new CommentData(userMichael, "Can I make it without cream?"))
            );
        }

        if (!recipeRepository.existsByTitle("Mumbai Pav Bhaji")) {
            createFullRecipe(
                    chefRahul, indian,
                    "Mumbai Pav Bhaji",
                    "Buttery Mumbai-style mixed vegetable bhaji mashed with pav bhaji masala and served with toasted pav.",
                    15, 25, 4, Difficulty.EASY, true, RecipeStatus.APPROVED,
                    "https://commons.wikimedia.org/wiki/Special:FilePath/Pav%20Bhaji.jpg?width=1200",
                    "https://www.youtube.com/watch?v=H7aGRi70wbU", 38,
                    List.of(
                            new String[]{"Potatoes", "3", "medium"},
                            new String[]{"Cauliflower", "1", "cup"},
                            new String[]{"Green Peas", "1", "cup"},
                            new String[]{"Capsicum", "1", "medium"},
                            new String[]{"Tomatoes", "3", "medium"},
                            new String[]{"Pav Bhaji Masala", "2", "tbsp"},
                            new String[]{"Butter", "3", "tbsp"},
                            new String[]{"Pav", "8", "pieces"}
                    ),
                    List.of(
                            new StepData(1, "Cook Vegetables", "Boil potatoes, cauliflower and peas until tender.", 12, "Cook until easy to mash."),
                            new StepData(2, "Make Bhaji Base", "Saute onion, capsicum and tomatoes with pav bhaji masala.", 8, "Cook until tomatoes become jammy."),
                            new StepData(3, "Mash", "Add boiled vegetables and mash everything together.", 5, "A potato masher gives the best texture."),
                            new StepData(4, "Finish Bhaji", "Add butter, adjust seasoning and simmer until glossy.", 5, "Add a splash of water if too thick."),
                            new StepData(5, "Toast Pav", "Butter the pav and toast on the tawa. Serve with bhaji, onion and lemon.", 3, "Serve piping hot.")
                    ),
                    List.of(userMichael), List.of(new RatingData(userMichael, 5, "Street-food comfort food.")),
                    List.of(new CommentData(userAlex, "Extra butter please!"))
            );
        }

        if (!recipeRepository.existsByTitle("Homestyle Aloo Gobi Masala")) {
            createFullRecipe(
                    chefRahul, indian,
                    "Homestyle Aloo Gobi Masala",
                    "Rustic dry curry of potatoes and cauliflower tossed with turmeric, cumin, coriander and fresh herbs.",
                    10, 25, 4, Difficulty.EASY, true, RecipeStatus.APPROVED,
                    "https://commons.wikimedia.org/wiki/Special:FilePath/Aloo%20Gobi%20Masala%20-%20Mohali%202016-08-07%208551.JPG?width=1200",
                    "https://www.youtube.com/watch?v=y7RUpvhET08", 35,
                    List.of(
                            new String[]{"Potatoes", "3", "medium"},
                            new String[]{"Cauliflower Florets", "3", "cups"},
                            new String[]{"Onion", "1", "large"},
                            new String[]{"Tomato", "1", "medium"},
                            new String[]{"Cumin Seeds", "1", "tsp"},
                            new String[]{"Turmeric Powder", "1/2", "tsp"},
                            new String[]{"Coriander Powder", "1", "tsp"},
                            new String[]{"Garam Masala", "1/2", "tsp"}
                    ),
                    List.of(
                            new StepData(1, "Temper Cumin", "Heat oil and crackle cumin seeds.", 1, "Do not burn the cumin."),
                            new StepData(2, "Cook Onion", "Add onion and saute until lightly golden.", 5, "Stir occasionally."),
                            new StepData(3, "Add Vegetables", "Add potatoes and cauliflower with turmeric and salt.", 8, "Toss gently to coat."),
                            new StepData(4, "Cook Covered", "Cover and cook until the vegetables are tender, stirring occasionally.", 12, "Add a splash of water if needed."),
                            new StepData(5, "Finish", "Add tomato, coriander powder and garam masala and cook until dry.", 5, "Finish with fresh coriander.")
                    ),
                    List.of(userElena), List.of(new RatingData(userElena, 5, "Simple and homestyle.")),
                    List.of(new CommentData(userSarah, "Perfect weekday lunch."))
            );
        }
    }

    private void updateVerifiedMedia(String title, String imageUrl, String videoUrl, int durationSec) {
        recipeRepository.findByTitle(title).ifPresent(recipe -> {
            recipe.setImageUrl(imageUrl);
            RecipeReel reel = recipe.getReel();
            if (reel == null) {
                reel = new RecipeReel();
                reel.setRecipe(recipe);
                recipe.setReel(reel);
            }
            reel.setVideoUrl(videoUrl);
            reel.setThumbnailUrl(imageUrl);
            reel.setDurationSeconds(durationSec);
            reel.setAspectRatio("9:16");
            reel.setStatus(recipe.getStatus());
            recipeRepository.save(recipe);
        });
    }

    private void initSettings() {
        SystemSetting websiteName = settingRepository.findBySettingKey("website_name").orElse(null);
        if (websiteName == null) {
            saveSetting("website_name", "CraveReel", "Website brand display name");
        } else if ("RecipeReels".equals(websiteName.getSettingValue())) {
            websiteName.setSettingValue("CraveReel");
            websiteName.setDescription("Website brand display name");
            settingRepository.save(websiteName);
        }
        saveSetting("max_video_size_mb", "100", "Max video upload size in MB");
        saveSetting("max_reel_duration_sec", "90", "Max reel duration in seconds");
        saveSetting("allow_comments", "true", "Allow users to post comments");
        saveSetting("allow_ratings", "true", "Allow users to rate and review recipes");
        saveSetting("allow_new_contributors", "true", "Allow registration as Contributor");
        saveSetting("content_moderation_mode", "MANUAL_APPROVAL", "Content moderation mode");
    }

    private void saveSetting(String key, String value, String desc) {
        if (!settingRepository.existsBySettingKey(key)) {
            settingRepository.save(new SystemSetting(key, value, desc));
        }
    }

    private void createFullRecipe(
            User contributor, Category category, String title, String description,
            int prepTime, int cookTime, int servings, Difficulty difficulty, boolean isVeg, RecipeStatus status,
            String imageUrl, String videoUrl, int durationSec,
            List<String[]> ingredients, List<StepData> steps,
            List<User> likers, List<RatingData> ratings, List<CommentData> comments) {

        Recipe recipe = new Recipe();
        recipe.setUser(contributor);
        recipe.setCategory(category);
        recipe.setTitle(title);
        recipe.setDescription(description);
        recipe.setPrepTimeMinutes(prepTime);
        recipe.setCookTimeMinutes(cookTime);
        recipe.setServings(servings);
        recipe.setDifficulty(difficulty);
        recipe.setVegetarian(isVeg);
        recipe.setStatus(status);
        recipe.setImageUrl(imageUrl);
        recipe.setViewCount((long) (Math.random() * 2500 + 450));

        Recipe savedRecipe = recipeRepository.save(recipe);

        // Add ingredients
        int o = 1;
        for (String[] ing : ingredients) {
            RecipeIngredient ingredient = new RecipeIngredient(ing[0], ing[1], ing[2], o++);
            savedRecipe.addIngredient(ingredient);
        }

        // Add steps
        for (StepData s : steps) {
            RecipeStep step = new RecipeStep(s.num, s.title, s.instruction, s.timerMinutes, s.tip);
            savedRecipe.addStep(step);
        }

        // Add reel
        RecipeReel reel = new RecipeReel();
        reel.setRecipe(savedRecipe);
        reel.setVideoUrl(videoUrl);
        reel.setThumbnailUrl(imageUrl);
        reel.setDurationSeconds(durationSec);
        reel.setStatus(status);
        reel.setAspectRatio("9:16");
        reel.setViewCount((long) (Math.random() * 5000 + 1200));
        savedRecipe.setReel(reel);

        Recipe persisted = recipeRepository.save(savedRecipe);

        // Likes
        for (User u : likers) {
            likeRepository.save(new RecipeLike(persisted, u));
        }

        // Ratings
        for (RatingData rd : ratings) {
            ratingRepository.save(new Rating(persisted, rd.user, rd.stars, rd.review));
        }

        // Comments
        for (CommentData cd : comments) {
            commentRepository.save(new Comment(persisted, cd.user, cd.content, null));
        }
    }

    private static class StepData {
        int num;
        String title;
        String instruction;
        int timerMinutes;
        String tip;

        StepData(int num, String title, String instruction, int timerMinutes, String tip) {
            this.num = num;
            this.title = title;
            this.instruction = instruction;
            this.timerMinutes = timerMinutes;
            this.tip = tip;
        }
    }

    private static class RatingData {
        User user;
        int stars;
        String review;

        RatingData(User user, int stars, String review) {
            this.user = user;
            this.stars = stars;
            this.review = review;
        }
    }

    private static class CommentData {
        User user;
        String content;

        CommentData(User user, String content) {
            this.user = user;
            this.content = content;
        }
    }
}
