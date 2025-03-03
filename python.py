def analyze_flow(
    graph: nx.DiGraph,
    matrix_orders: List[List[int]],
    matrix_corridors: List[List[int]],
    max_len_combinations: int = 2,
    max_combinations: int = 500,

) -> Tuple[int, List[int], List[int], int, bool]:
    """Analisa o fluxo para determinar quais pedidos foram concluídos e quais
    corredores foram usados."""
    global MEMORY_ITEMS, MEMORY_CORRIDORS, MEMORY_ORDERS, REPEAT
    global LEN_ITENS, MAX_LEN_COMBINATION, VERBOSE, LB


    if graph is None:
        print("\033[1m\033[91mNo flow found.\033[0m")
        return None, None, None, None, False

    removed_total_flow = 0

    used_corridors = set()

    #para saber quais corredores são usados, basta saber se o fluxo por ele é diferente de zero
    for corridor in MEMORY_CORRIDORS:
        if graph[corridor]["sink"]["flow"] > 0:
            used_corridors.add(int(corridor.replace("corridor_", "")))


    total_available = [0] * LEN_ITENS
    for i in used_corridors:
        total_available = sum_vector(total_available, matrix_corridors[i])
    
    orders_completed, _, total_required = find_completed_orders(graph, matrix_orders)

    used_corridors, total_available, removed_total_flow = remove_unnecessary_corridors(
        used_corridors, total_available, total_required, matrix_corridors, orders_completed, graph, max_combinations, max_len_combinations
    )

    #como o programador é burro e não sabia que estava funcionado colou esse print
    for i in range(len(total_required)):
        if total_required[i] > total_available[i]:
            print(f"\033[1m\033[91mItem {i} is missing in the corridors\033[0m")
            return sum(total_required), list(used_corridors), orders_completed, removed_total_flow, True
    
    return sum(total_required), list(used_corridors), orders_completed, removed_total_flow, False






def remove_unnecessary_corridors(
    corridors_set: Set[int],
    total_available: List[int],
    total_required: List[int],
    matrix_corridors: List[List[int]],
    orders_completed: List[int],
    graph: nx.DiGraph,
    max_combinations: int = 500,
    max_len_combinations: int = 2,
) -> Tuple[Set[int], List[int], int]:
    global VERBOSE, REPEAT, MAX_LEN_COMBINATION, LB, LEN_ITENS

    #TODO criar um esquema de memória... dado uma conjunto de corredores que eu quero remover
    # e um ``total_required`` eu posso saber se é possível remover esses corredores...

    # para limitar a max_combinations combinações
    max_len_combinations = max(MAX_LEN_COMBINATION, max_len_combinations)

    total_of_combinations = (2 ** MAX_LEN_COMBINATION - 1) * len(corridors_set)
    if total_of_combinations > max_combinations:
        max_len_combinations = int(log2(max_combinations // len(corridors_set) + 1))
        total_of_combinations = (2 ** max_len_combinations - 1) * len(corridors_set)

    r_memory = 0
    found_combinations = False
    removed_total_flow = 0

    for r in range(MAX_LEN_COMBINATION, 0, -1):
        if len(corridors_set) == 1 or found_combinations or not orders_completed or sum(total_required) < LB:
            break
        r_memory = r
        for combo in combinations(corridors_set, r):
            total_combo = [0] * LEN_ITENS
            for corridor in combo:
                total_combo = sum_vector(total_combo, matrix_corridors[corridor])
            remove = all(
                total_available[i] - total_combo[i] >= total_required[i]
                for i in range(LEN_ITENS)
            )

            if remove:
                if VERBOSE:
                    print(f"\033[1m\033[94m{len(combo)}/{len(corridors_set)} is unnecessary \033[0m")
                for corridor in combo:
                    corridors_set.discard(corridor)
                    aux = [-1 * x for x in matrix_corridors[corridor]]
                    total_available = sum_vector(total_available, aux)
                    # Uncomment the following line if the remove_corridor function is fixed
                    # removed_total_flow += remove_corridor(graph, f"corridor_{corridor}", orders_completed)
                REPEAT = 1
                found_combinations = True
                break

    #"maquina de estados" que tenta estimar o max comprimento da combinação
    #TODO da para melhor isso e fazer com base na lista de corredores que recebo...
    #para cada combinação de corredores que recebo, eu tenho um valor de MAX_LEN_COMBINATION
    #e inicialmente eu tenho um valor de MAX_LEN_COMBINATION = 2

    if r_memory >= MAX_LEN_COMBINATION:
        MAX_LEN_COMBINATION = r_memory + 1
    else:
        MAX_LEN_COMBINATION = max(1, r_memory - 1)

    return corridors_set, total_available, removed_total_flow



def find_completed_orders(graph: nx.DiGraph, matrix_orders: List[List[int]]) -> Tuple[List[int], List[int], List[int]]:
    """Encontra os pedidos completos e incompletos com base no fluxo do grafo."""
    global MEMORY_ORDERS, SOURCE, LEN_ITENS, TOTAL_ITENS_REQUIRED

    orders_completed = []
    orders_incomplete = []
    total_required = [0] * LEN_ITENS
    #para saber quais pedidos estão completos, basta saber se o fluxo por ele é igual a capacidade
    for order in MEMORY_ORDERS:
        idx = int(order.replace("order_", ""))
        if graph[SOURCE][order]["flow"] == graph[SOURCE][order]["capacity"]:
            orders_completed.append(idx)
            total_required = sum_vector(total_required, matrix_orders[idx])
        elif graph[SOURCE][order]["flow"] != 0: 
           # o pedido não está completo, mas pode estar usando corredor desnecessário
           orders_incomplete.append(idx)

    return orders_completed, orders_incomplete, total_required