package com.gharse.admin;

import com.gharse.availability.Batch;
import com.gharse.availability.BatchDishItem;
import com.gharse.availability.BatchRepository;
import com.gharse.cook.ApprovalStatus;
import com.gharse.cook.Cook;
import com.gharse.cook.CookRepository;
import com.gharse.cuisine.CuisineRepository;
import com.gharse.kitchen.Kitchen;
import com.gharse.kitchen.KitchenRepository;
import com.gharse.menu.Dish;
import com.gharse.menu.DishCategory;
import com.gharse.menu.DishRepository;
import com.gharse.order.Order;
import com.gharse.order.OrderItem;
import com.gharse.order.OrderRepository;
import com.gharse.order.OrderStatus;
import com.gharse.order.OrderType;
import com.gharse.payment.PaymentStatus;
import com.gharse.review.Review;
import com.gharse.review.ReviewRepository;
import com.gharse.user.User;
import com.gharse.user.UserRepository;
import com.gharse.user.UserRole;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DEMO DATA ONLY. Enabled with gharse.seed.enabled=true (on by default in the `local` profile, never in prod).
 * Every seeded account uses an @gharse.demo email and the password {@value #PASSWORD}. Safe to run repeatedly.
 */
@Component
@org.springframework.core.annotation.Order(2)
@ConditionalOnProperty(name = "gharse.seed.enabled", havingValue = "true")
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    static final String PASSWORD = "Demo@1234";
    private static final String MARKER_EMAIL = "admin@gharse.demo";

    private final UserRepository users;
    private final CookRepository cooks;
    private final KitchenRepository kitchens;
    private final CuisineRepository cuisines;
    private final DishRepository dishes;
    private final BatchRepository batches;
    private final OrderRepository orders;
    private final ReviewRepository reviews;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final ZoneId zone;

    public DemoDataSeeder(UserRepository users, CookRepository cooks, KitchenRepository kitchens, CuisineRepository cuisines,
                          DishRepository dishes, BatchRepository batches, OrderRepository orders, ReviewRepository reviews,
                          PasswordEncoder encoder, Clock clock, ZoneId zone) {
        this.users = users;
        this.cooks = cooks;
        this.kitchens = kitchens;
        this.cuisines = cuisines;
        this.dishes = dishes;
        this.batches = batches;
        this.orders = orders;
        this.reviews = reviews;
        this.encoder = encoder;
        this.clock = clock;
        this.zone = zone;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmail(MARKER_EMAIL)) {
            log.info("Demo data already present, skipping seed");
            return;
        }
        log.warn("Seeding DEMO data (accounts @gharse.demo, password {})", PASSWORD);
        user("CUOQO Demo Admin", MARKER_EMAIL, "9000000001", UserRole.ADMIN);
        User priya = user("Priya Kulkarni", "priya@gharse.demo", "9000000002", UserRole.CUSTOMER);
        User rahul = user("Rahul Joshi", "rahul@gharse.demo", "9000000003", UserRole.CUSTOMER);
        priya.setAddress("Flat 12, Sunrise Apartments, Kothrud, Pune 411038");
        rahul.setAddress("B-204, Green Park, Baner, Pune 411045");

        Cook meena = cook("Meena Patil", "meena@gharse.demo", "Kothrud", 18.5074, 73.8077, "Authentic Kolhapuri Food", 25,
                "Cooking has been part of my family for over 25 years. My Aai taught me to grind masalas on the stone, and "
                        + "every Sunday our home smelled of Puran Poli. Now I cook the same food for your family.",
                "Puran Poli, Misal Pav, Maharashtrian Thali", "Maharashtrian", "Kolhapuri");
        Cook asha = cook("Asha Sharma", "asha@gharse.demo", "Baner", 18.5590, 73.7868, "Punjabi comfort food, slow-cooked", 15,
                "I moved to Pune from Amritsar after marriage and never stopped making my mother's rajma and parathas. "
                        + "Everything is cooked in desi ghee, the way it should be.",
                "Rajma Chawal, Aloo Paratha, Dal Makhani", "North Indian");
        Cook sneha = cook("Sneha Iyer", "sneha@gharse.demo", "Aundh", 18.5580, 73.8075, "Filter coffee, idlis and Tamil home meals", 12,
                "Fluffy idlis, crisp dosas and sambar that simmers for hours. Batter ground fresh every morning.",
                "Idli Sambar, Masala Dosa, Curd Rice", "South Indian");
        Cook kavita = cook("Kavita Banerjee", "kavita@gharse.demo", "Viman Nagar", 18.5679, 73.9143, "Bengali kitchen: fish, sweets, nostalgia", 20,
                "A Kolkata girl in Pune. My kitchen is full of mustard oil, panch phoron and mishti. Order early, my rosogolla sells out.",
                "Fish Curry, Luchi Aloor Dom, Rosogolla", "Bengali", "Sweets");
        Cook ramesh = cook("Ramesh Shaikh", "ramesh@gharse.demo", "Koregaon Park", 18.5362, 73.8940, "Dum biryani and fresh-baked cakes", 18,
                "I learned dum biryani from my ustaad in Hyderabad and baking from my daughter's YouTube channel. Small batches, big flavour.",
                "Chicken Dum Biryani, Veg Biryani, Eggless Chocolate Cake", "Biryani", "Bakery", "Snacks");

        Dish poli = dish(meena, "Puran Poli", "Maharashtrian", DishCategory.SWEET, 120, "4 pieces", true, "Sweet flatbread stuffed with chana dal and jaggery, served with ghee.");
        Dish misal = dish(meena, "Misal Pav", "Kolhapuri", DishCategory.SNACK, 110, "1 plate with 2 pav", true, "Fiery sprouted-moth curry topped with farsan, onion and lemon.");
        Dish mthali = dish(meena, "Maharashtrian Thali", "Maharashtrian", DishCategory.THALI, 220, "Full thali", true, "Varan bhaat, bharli vangi, koshimbir, chapati, amti and a sweet.");
        Dish rajma = dish(asha, "Rajma Chawal", "North Indian", DishCategory.MEAL, 150, "1 bowl + rice", true, "Slow-cooked kidney beans in a tomato-onion gravy.");
        Dish paratha = dish(asha, "Aloo Paratha", "North Indian", DishCategory.MEAL, 90, "2 parathas with curd", true, "Stuffed with spiced potato, cooked on a tawa with butter.");
        Dish dal = dish(asha, "Dal Makhani", "North Indian", DishCategory.MEAL, 170, "500 ml", true, "Black lentils simmered overnight with cream and butter.");
        Dish idli = dish(sneha, "Idli Sambar", "South Indian", DishCategory.MEAL, 80, "4 idlis", true, "Soft idlis with sambar and two chutneys.");
        Dish dosa = dish(sneha, "Masala Dosa", "South Indian", DishCategory.MEAL, 100, "2 dosas", true, "Crisp dosa with potato masala, sambar and chutney.");
        Dish curd = dish(sneha, "Curd Rice", "South Indian", DishCategory.MEAL, 70, "1 bowl", true, "Cooling curd rice tempered with mustard and curry leaves.");
        Dish fish = dish(kavita, "Macher Jhol", "Bengali", DishCategory.MEAL, 260, "2 pieces + rice", false, "Rohu in a light mustard-and-turmeric gravy.");
        Dish luchi = dish(kavita, "Luchi Aloor Dom", "Bengali", DishCategory.MEAL, 140, "4 luchis", true, "Puffed fried bread with spiced baby potato curry.");
        Dish roso = dish(kavita, "Rosogolla", "Sweets", DishCategory.SWEET, 180, "6 pieces", true, "Spongy chhena balls in light sugar syrup.");
        Dish biryani = dish(ramesh, "Chicken Dum Biryani", "Biryani", DishCategory.MEAL, 280, "1 portion (serves 1-2)", false, "Layered, sealed and slow-cooked on dum.");
        Dish vegBiryani = dish(ramesh, "Veg Biryani", "Biryani", DishCategory.MEAL, 200, "1 portion", true, "Seasonal vegetables and saffron rice, dum style.");
        Dish cake = dish(ramesh, "Eggless Chocolate Cake", "Bakery", DishCategory.BAKERY, 650, "500 g cake", true, "Moist dark chocolate cake with ganache.");
        Dish samosa = dish(ramesh, "Punjabi Samosa", "Snacks", DishCategory.SNACK, 60, "2 pieces", true, "Crisp pastry with spiced potato and peas.");
        Dish thepla = dish(asha, "Methi Thepla Pack", "North Indian", DishCategory.SNACK, 100, "8 theplas", true, "Travel-friendly methi theplas with pickle.");
        Dish khichdi = dish(meena, "Sabudana Khichdi", "Maharashtrian", DishCategory.SNACK, 90, "1 plate", true, "Fasting-friendly, with peanuts and cumin.");

        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant now = clock.instant();
        Instant soon = now.plus(Duration.ofHours(6));
        // Open batches (today and upcoming), some nearly sold out
        Batch b1 = batch(meena, "Sunday Special", "Puran Poli made fresh for Sunday lunch", today.plusDays(1), dayBefore(today.plusDays(1)), 30, 18, List.of(poli, khichdi));
        Batch b2 = batch(meena, "Today's Misal", "Misal pav, ready at lunch", today, soon, 25, 9, List.of(misal));
        Batch b3 = batch(asha, "Punjabi Lunch Box", "Rajma chawal and dal makhani", today, soon, 20, 15, List.of(rajma, dal));
        Batch b4 = batch(asha, "Paratha Morning", "Hot stuffed parathas", today.plusDays(2), dayBefore(today.plusDays(2)), 15, 4, List.of(paratha, thepla));
        Batch b5 = batch(sneha, "South Indian Breakfast", "Idli and dosa batter-fresh", today.plusDays(1), dayBefore(today.plusDays(1)), 40, 12, List.of(idli, dosa, curd));
        Batch b6 = batch(sneha, "Saturday Dosa", "Masala dosa to your door", today.plusDays(3), dayBefore(today.plusDays(3)), 20, 0, List.of(dosa));
        Batch b7 = batch(kavita, "Bengali Bhoj", "Macher jhol and luchi", today.plusDays(2), dayBefore(today.plusDays(2)), 12, 10, List.of(fish, luchi));
        Batch b8 = batch(kavita, "Mishti Box", "Fresh rosogolla, order early", today.plusDays(1), dayBefore(today.plusDays(1)), 24, 21, List.of(roso));
        Batch b9 = batch(ramesh, "Biryani Saturday", "Dum biryani, sealed handi", today, soon, 30, 22, List.of(biryani, vegBiryani));
        Batch b10 = batch(ramesh, "Cake Order Window", "Eggless chocolate cakes and samosas", today.plusDays(4), dayBefore(today.plusDays(4)), 10, 2, List.of(cake, samosa));
        List<Batch> all = List.of(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10);
        all.forEach(batches::save);

        // Completed orders with reviews so ratings are visible immediately
        completedOrder(priya, meena, b1, poli, 2, 5, 5, 4, 5, "Tastes exactly like my grandmother's. The ghee was perfect!");
        completedOrder(rahul, meena, b2, misal, 1, 5, 5, 5, 4, "Proper Kolhapuri heat. Will order again.");
        completedOrder(priya, meena, b2, misal, 2, 4, 4, 5, 4, "Great misal, pav could be softer.");
        completedOrder(rahul, asha, b3, rajma, 1, 5, 5, 5, 5, "Best rajma I've had in Pune.");
        completedOrder(priya, asha, b3, dal, 1, 4, 4, 4, 4, "Rich and creamy.");
        completedOrder(priya, sneha, b5, idli, 2, 5, 5, 5, 5, "Soft idlis and wonderful sambar.");
        completedOrder(rahul, kavita, b7, fish, 1, 5, 5, 4, 5, "Reminded me of home in Kolkata.");
        completedOrder(rahul, ramesh, b9, biryani, 1, 5, 5, 5, 5, "Dum biryani done right.");
        completedOrder(priya, ramesh, b9, vegBiryani, 1, 4, 4, 5, 4, "Fragrant and well spiced.");
        refreshRatings(List.of(meena, asha, sneha, kavita, ramesh));
        log.warn("Demo data ready. Log in with e.g. priya@gharse.demo / meena@gharse.demo / admin@gharse.demo (password {})", PASSWORD);
    }

    private Instant dayBefore(LocalDate day) {
        return day.minusDays(1).atTime(20, 0).atZone(zone).toInstant();
    }

    private User user(String name, String email, String phone, UserRole role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setRole(role);
        u.setPasswordHash(encoder.encode(PASSWORD));
        return users.save(u);
    }

    private Cook cook(String name, String email, String locality, double lat, double lng, String headline, int years,
                      String bio, String signature, String... cuisineNames) {
        User u = user(name, email, "98900" + (10000 + users.count()), UserRole.COOK);
        Cook c = new Cook();
        c.setUser(u);
        c.setHeadline(headline);
        c.setBio(bio);
        c.setYearsOfCooking(years);
        c.setLocality(locality);
        c.setSignatureDishes(signature);
        c.setApprovalStatus(ApprovalStatus.APPROVED);
        for (String cn : cuisineNames) cuisines.findByNameIgnoreCase(cn).ifPresent(c.getCuisines()::add);
        c = cooks.save(c);
        Kitchen k = new Kitchen();
        k.setCook(c);
        k.setAddress("Demo kitchen, " + locality + ", Pune");
        k.setLocality(locality);
        k.setLatitude(lat);
        k.setLongitude(lng);
        k.setDescription("A clean, well-kept home kitchen in " + locality + ".");
        k.setVerificationStatus(ApprovalStatus.APPROVED);
        kitchens.save(k);
        return c;
    }

    private Dish dish(Cook cook, String name, String cuisine, DishCategory category, int price, String portion, boolean veg, String description) {
        Dish d = new Dish();
        d.setCook(cook);
        d.setName(name);
        d.setDescription(description);
        d.setCategory(category);
        d.setCuisine(cuisines.findByNameIgnoreCase(cuisine).orElse(null));
        d.setPrice(BigDecimal.valueOf(price));
        d.setPortionDescription(portion);
        d.setPreparationTimeMinutes(45);
        d.setVegetarian(veg);
        return dishes.save(d);
    }

    private Batch batch(Cook cook, String name, String description, LocalDate date, Instant cutoff, int capacity, int reserved, List<Dish> items) {
        Batch b = new Batch();
        b.setCook(cook);
        b.setName(name);
        b.setDescription(description);
        b.setAvailableDate(date);
        b.setOrderCutoff(cutoff);
        b.setCapacity(capacity);
        b.setReservedQuantity(reserved);
        b.setPickupStart(LocalTime.of(12, 0));
        b.setPickupEnd(LocalTime.of(14, 0));
        b.setDeliveryStart(LocalTime.of(12, 0));
        b.setDeliveryEnd(LocalTime.of(15, 0));
        items.forEach(d -> b.getDishes().add(new BatchDishItem(d, d.getPrice())));
        return b;
    }

    private void completedOrder(User customer, Cook cook, Batch batch, Dish dish, int qty, int rating, int taste, int hygiene,
                                int reliability, String comment) {
        Order o = new Order();
        o.setCustomer(customer);
        o.setCook(cook);
        o.setOrderType(OrderType.PICKUP);
        o.setOrderStatus(OrderStatus.COMPLETED);
        o.setPaymentStatus(PaymentStatus.PAID);
        BigDecimal total = dish.getPrice().multiply(BigDecimal.valueOf(qty));
        o.setSubtotal(total);
        o.setTotalAmount(total);
        o.setOrderNumber("GS" + orders.nextOrderNumber());
        OrderItem item = new OrderItem();
        item.setOrder(o);
        item.setDish(dish);
        item.setBatch(batch);
        item.setDishNameSnapshot(dish.getName());
        item.setUnitPrice(dish.getPrice());
        item.setQuantity(qty);
        item.setTotalPrice(total);
        o.getItems().add(item);
        o = orders.save(o);
        Review r = new Review();
        r.setOrderId(o.getId());
        r.setCustomer(customer);
        r.setCook(cook);
        r.setRating(rating);
        r.setTasteRating(taste);
        r.setHygieneRating(hygiene);
        r.setPackagingRating(4);
        r.setReliabilityRating(reliability);
        r.setComment(comment);
        reviews.save(r);
    }

    private void refreshRatings(List<Cook> list) {
        reviews.flush();
        for (Cook c : new ArrayList<>(list)) {
            Double avg = reviews.averageRating(c.getId());
            c.setRating(BigDecimal.valueOf(avg == null ? 0 : avg).setScale(2, RoundingMode.HALF_UP));
            c.setReviewCount((int) reviews.countByCookId(c.getId()));
        }
    }
}
