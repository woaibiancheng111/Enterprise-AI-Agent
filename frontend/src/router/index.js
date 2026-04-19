import { createRouter, createWebHistory } from "vue-router";
import AgentWorkbench from "../views/AgentWorkbench.vue";
const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: "/",
            name: "workbench",
            component: AgentWorkbench
        }
    ]
});
export default router;
