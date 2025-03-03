import os
import sys
import shutil
from checker import WaveOrderPicking
from typing import *

dir_best_solution = "../best_solution"


def update_best_solution(path_input: str, path_possible_solution: str, path_best_solution: str) -> Tuple[int, int]:
    wave_order_picking = WaveOrderPicking()

    wave_order_picking.read_input(path_input)
    selected_orders, visited_aisles = wave_order_picking.read_output(path_possible_solution)
    is_feasible = wave_order_picking.is_solution_feasible(selected_orders, visited_aisles)
    best_objective_value, current_objective_value = -1, -1

    if os.path.exists(path_best_solution):
        wave_order_picking.read_input(path_input)
        best_selected_orders, best_visited_aisles = wave_order_picking.read_output(path_best_solution)
        is_feasible = wave_order_picking.is_solution_feasible(best_selected_orders, best_visited_aisles)
        best_objective_value = wave_order_picking.compute_objective_function(
            best_selected_orders, best_visited_aisles
    )

    if is_feasible:
        current_objective_value = wave_order_picking.compute_objective_function(selected_orders, visited_aisles)
        best_objective_value = 0
        if os.path.exists(path_best_solution):
            wave_order_picking.read_input(path_input)
            best_selected_orders, best_visited_aisles = wave_order_picking.read_output(path_best_solution)
            is_feasible = wave_order_picking.is_solution_feasible(best_selected_orders, best_visited_aisles)
            best_objective_value = wave_order_picking.compute_objective_function(
                best_selected_orders, best_visited_aisles
            )

        if best_objective_value < current_objective_value:
            print("\033[92m\033[1mNew solution is better. Updating best solution.\033[0m")
            if not os.path.exists(dir_best_solution):
                os.mkdir(dir_best_solution)
            
            temp_path = path_best_solution + ".tmp"
            with open(temp_path, "w") as file:
                file.write(f"{len(selected_orders)}\n")
                for order in selected_orders:
                    file.write(f"{order}\n")
                file.write(f"{len(visited_aisles)}\n")
                for aisle in visited_aisles:
                    file.write(f"{aisle}\n")

            shutil.move(temp_path, path_best_solution)
        else:
            if best_objective_value == current_objective_value:
                print("\033[94mCurrent best solution is equal. No update needed.\033[0m")
            else:
                print("\033[93mPrevious solution is better. No update needed.\033[0m")
    else:
        print("Possible solution is not feasible. No update performed.")

    return best_objective_value, current_objective_value


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: update_best_solution.py <path_input> <path_possible_solution>")
        sys.exit(1)

    path_input = sys.argv[1]
    path_possible_solution = sys.argv[2]
    path_best_solution = path_possible_solution.replace("possible", "best")

    update_best_solution(path_input, path_possible_solution, path_best_solution)
