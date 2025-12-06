package com.synapse.social.studioasinc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.synapse.social.studioasinc.ui.home.FeedScreen
import com.synapse.social.studioasinc.ui.reels.ReelsScreen
import com.synapse.social.studioasinc.ui.notifications.NotificationsScreen

sealed class HomeDestinations(val route: String) {
    object Feed : HomeDestinations("feed")
    object Reels : HomeDestinations("reels")
    object Notifications : HomeDestinations("notifications")
    object PostDetail : HomeDestinations("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    object Profile : HomeDestinations("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}

@Composable
fun HomeNavGraph(
    navController: NavHostController,
    startDestination: String = HomeDestinations.Feed.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(HomeDestinations.Feed.route) {
            FeedScreen(
                onPostClick = { postId -> navController.navigate(HomeDestinations.PostDetail.createRoute(postId)) },
                onUserClick = { userId -> navController.navigate(HomeDestinations.Profile.createRoute(userId)) },
                onCommentClick = { postId -> navController.navigate(HomeDestinations.PostDetail.createRoute(postId)) }, // Typically opens detail with comments
                onMediaClick = { /* Launch media viewer */ }
            )
        }

        composable(HomeDestinations.Reels.route) {
             ReelsScreen(
                 onUserClick = { userId -> navController.navigate(HomeDestinations.Profile.createRoute(userId)) },
                 onCommentClick = { /* Open comments */ }
             )
        }

        composable(HomeDestinations.Notifications.route) {
             NotificationsScreen(
                 onNotificationClick = { notification ->
                     // Handle navigation based on notification type
                 },
                 onUserClick = { userId -> navController.navigate(HomeDestinations.Profile.createRoute(userId)) }
             )
        }

        // Placeholders for destinations managed outside this graph or to be implemented
        composable(HomeDestinations.PostDetail.route) { backStackEntry ->
             // PostDetailScreen(postId = backStackEntry.arguments?.getString("postId"))
        }

        composable(HomeDestinations.Profile.route) { backStackEntry ->
             // ProfileScreen(userId = backStackEntry.arguments?.getString("userId"))
        }
    }
}
