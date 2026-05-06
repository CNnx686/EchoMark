import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import DiscoveryView from "../views/DiscoveryView.vue";
import UserInformationView from "@/views/UserInformationView.vue";
import NotificationView from "@/views/NotificationView.vue";
import LoginInView from "@/views/LoginInView.vue";
import RegisterView from "@/views/RegisterView.vue";
import ForgotPasswordView from "@/views/ForgotPasswordView.vue";
import ResetPasswordView from "@/views/ResetPasswordView.vue";
import AudioDetailView from "@/views/AudioDetailView.vue";

import MyFavourite from "@/views/MyFavourite.vue";

const router = createRouter({
  history: createWebHistory("/"),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
    {
      path: "/discovery",
      name: "discovery",
      component: DiscoveryView,
    },
    {
      path: "/notification",
      name: "notification",
      component: NotificationView,
    },
    {
      path: "/userInformation/:userId",
      name: "userInformation",
      component: UserInformationView,
      props: true,
    },
    {
      path: "/login",
      name: "login",
      component: LoginInView,
    },
    {
      path: "/register",
      name: "register",
      component: RegisterView,
    },
    {
      path: "/forgetPassword",
      name: "forgetPassword",
      component: ForgotPasswordView,
    },
    {
      path: "/resetPassword",
      name: "resetPassword",
      component: ResetPasswordView,
    },
    {
      path: "/audio/:audioId",
      name: "audioDetail",
      component: AudioDetailView,
      props: true,
    },
    {
      path: "/MyFavourite",
      name: "MyFavourite",
      component: MyFavourite,
    },
  ],
});

export default router;
