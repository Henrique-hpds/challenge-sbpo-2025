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





def state_machine(
    graph: nx.DiGraph,
    lb: int,
    ub: int,
    file_path: str,
    matrix_orders: List[List[int]],
    matrix_corridors: List[List[int]]
) -> Tuple[int, nx.DiGraph]:
    """
    Calcula o fluxo de custo mínimo respeitando os limites inferior (lb) e superior (ub).
    Utiliza uma abordagem iterativa com otimizações baseadas em heurísticas e resets estratégicos.
    """
    global START_TIME, LEN_CORRIDORS, MEMORY_CORRIDORS
    global BEST_GRAPH, BEST_TOTAL_FLOW, COUNTER, MAX_ITERATIONS
    global BEST_RATIO, TIME_BEST_RATIO, RESTART_GRAPH, VERBOSE
    global RESET_THRESHOLD, THRESHOLD_LOOP_CORRIDORS
    global HARD_RESET, MAX_HARD_RESET, REPEAT

    total_flow = 0
    residual_graph = init_residual_graph(graph)

    # Inicializa dicionários para armazenar fluxos de itens e corredores
    iteration_count = 0
    best_iteration = float("inf")
    list_corridors = []
    current_ratio = 0
    previous_ratio = -1

    # Armazena o melhor estado encontrado até o momento
    BEST_GRAPH = residual_graph.copy()
    BEST_TOTAL_FLOW = 0
    BEST_RATIO = 0
    history = []
    previous_best_solution = time.time()

    maximum_flow_enable = False

    max_ub = 5
    counter_ub = 0

    # Executa a iteração até atingir o limite de tempo ou o critério de parada
    try:
        while True:
            ################### EXPANSÃO EM PRIORIDADES################
            # Tenta aumentar o fluxo no grafo residual por meio de prioridade

            parent = priority_choice(residual_graph)
            if not parent:
                print("\033[1m\033[91mNo augmenting path found.\033[0m")
                break

            total_flow = augment_flow(residual_graph, parent, total_flow, ub)

            ################## EXPANSÃO BASEADA EM CORREDORES ##################
            list_corridors = []
            for corridor in MEMORY_CORRIDORS:
                if residual_graph[corridor][SINK]["flow"] != 0:
                    list_corridors.append(int(corridor.replace("corridor_", "")))
            
            new_total_flow = expand_flow_by_corridors(residual_graph, total_flow, list_corridors, ub)
            aux = 0
            for corridor in list_corridors:
                aux += residual_graph[f"corridor_{corridor}"]["sink"]["flow"]

            #talvez seja interassante não saturar o corredor, mas sim tentar aumentar o fluxo até um certo ponto
            counter = 0
            THRESHOLD_LOOP_CORRIDORS = 1000000
            while new_total_flow > total_flow and counter < THRESHOLD_LOOP_CORRIDORS:
                COUNTER += 1
                total_flow = new_total_flow
                new_total_flow = expand_flow_by_corridors(residual_graph, total_flow, list_corridors, ub)
                iteration_count+=1
                counter += 1
            
            ################## RESET DE GRAFO ##################
            #TODO levar em conta a quantidade de corredores uteis, pois isso influencia o quanto o valor objetivo pode piorar

            #TODO... Há casos em que o fluxo esta muito superior a LB, porém a quantidade de items que estão sendo coletados é baixa,
            #talvez valha a pena criar parâmetros que detectem isso e tratem esse caso 

            # Se a razão atual é menor que 75% da melhor razão e já passou 60 segundos
            bool_reset = (time.time() - TIME_BEST_RATIO > 60) and (
                BEST_RATIO != 0 and (current_ratio / BEST_RATIO) < 0.75)  
            # O fluxo total atual é significativamente maior que o melhor fluxo total e sem a melhor razão
            bool_reset = bool_reset or (((total_flow - BEST_TOTAL_FLOW) > ub // 5) and BEST_RATIO != 0 and current_ratio / BEST_RATIO < 0.8)  
            # Ou a razão atual é muito menor que a melhor razão
            bool_reset = bool_reset or (BEST_RATIO != 0 and (current_ratio / BEST_RATIO) < 0.2)  
            # E a melhor iteração não é infinito
            bool_reset = bool_reset and best_iteration != float("inf")  
            # E a razão atual é menor que 90% da melhor razão
            bool_reset = (bool_reset and BEST_RATIO != 0 and (current_ratio / BEST_RATIO) < 0.9) 
            # E o fluxo total atual é significativamente maior que o melhor fluxo total
            bool_reset = (bool_reset and (total_flow - BEST_TOTAL_FLOW) > ub // 10)
            # Ou se o tempo está prestes a acabar
            bool_reset = bool_reset or (time.time() - START_TIME >= STOP_TIME * 0.90)  
            bool_reset = bool_reset or (
                previous_ratio != 0
                and abs(current_ratio / previous_ratio) < 0.3
                and current_ratio != 0
                and previous_ratio > 10
                and total_flow > lb * 1.2
            )  # E a razão atual é menor que 90% da melhor razão

            bool_reset = bool_reset and not (current_ratio > previous_ratio * 1.03)  # Nunca resetar aumentar 5% a razão
            bool_reset = bool_reset and not (current_ratio >= 0.9 * BEST_RATIO)
            bool_reset = bool_reset and not (current_ratio <= 0.5)
            # ub > 100 para evitar resetar o grafo quando o fluxo é muito baixo 
            # Reseto o grafo quando o fluxo atinge 90% do fluxo máximo permitido, mas so no "inicio do código"
            # pois ao final do código posso permitir retornar o fluxo numa tentativa de

            if total_flow >= 0.9 * ub \
                and not maximum_flow_enable:
                bool_reset = True

            #definir melhor esse valor de RESET_THRESHOLD
            if BEST_ITERATION + RESET_THRESHOLD < iteration_count and BEST_RATIO != 0:
                bool_reset = True
                RESTART_GRAPH = 10000  #isso força o hard reset

            if bool_reset:  
                total_flow, best_iteration, current_ratio, residual_graph = reset_graph(
                    graph,
                    residual_graph,
                    iteration_count,
                    current_ratio,
                    list_corridors,
                    ub,
                    total_flow,
                    matrix_corridors,
                    matrix_orders
                )

            ################## AVALIAÇÃO DA MELHORA ##################
            if lb <= total_flow:
                total_items, list_corridors, _, _, reset_graph_bool = analyze_flow(
                    residual_graph, matrix_orders, matrix_corridors)
                # total_flow = total_items + removed_total_flow
                if reset_graph_bool:
                    total_flow, best_iteration, current_ratio, residual_graph = reset_graph(
                        graph,
                        residual_graph,
                        iteration_count,
                        current_ratio,
                        list_corridors,
                        ub,
                        total_flow,
                        matrix_corridors,
                        matrix_orders
                    )

                elif len(list_corridors) != 0:
                    if ub >= total_items >= lb:
                        current_ratio = total_items / len(list_corridors)
                        if current_ratio > BEST_RATIO:
                            previous_best_solution = TIME_BEST_RATIO
                            copy_correct_flow(
                                total_flow, residual_graph, current_ratio, iteration_count
                            )
                            best_iteration = iteration_count


            ###################### HISTÓRICO ######################################
            if total_flow < lb:
                current_ratio = 0
            previous_ratio = current_ratio
            history.append([total_flow, current_ratio, time.time() - START_TIME, iteration_count])

            ## debugging
            if total_flow < 0:
                print("\033[91mError in total flow.\033[0m")
                break
            ## debugging

            ################## CRITÉRIOS DE PARADA ##################

            if time.time() - START_TIME >= STOP_TIME * 0.95:
                print("\033[1m\033[91mTime limit is about to be reached, stopping the algorithm.\033[0m")
                break
            if iteration_count > MAX_ITERATIONS:
                print("\033[1m\033[91mReached maximum number of iterations, stopping the algorithm.\033[0m")
                break
            if HARD_RESET >= MAX_HARD_RESET:
                print("\033[91mHard reset limit reached, stopping the algorithm.\033[0m")
                break
            iteration_count += 1

            if VERBOSE and iteration_count % ((LATENCY // 10) + 1) * 10 == 0:
                print(f"Iteration {iteration_count}: Total flow: {total_flow} | Current ratio {current_ratio:.2f} | Best ratio: {BEST_RATIO:.2f} ({(time.time() - START_TIME):.2f}s)")

            if total_flow >= ub:
                counter_ub += 1
                RESTART_GRAPH = 10000

                total_flow, best_iteration, current_ratio, residual_graph = reset_graph(
                    graph,
                    residual_graph,
                    iteration_count,
                    current_ratio,
                    list_corridors,
                    ub,
                    total_flow,
                    matrix_corridors,
                    matrix_orders
                )
                if counter_ub > max_ub:
                    print("\033[1m\033[91mReached upper bound, stopping the algorithm.\033[0m")
                    break

            # Condições para habilitar o fluxo máximo
            if (BEST_RATIO != 0):
                if (time.time() - TIME_BEST_RATIO > 1.2 * (TIME_BEST_RATIO - previous_best_solution) and maximum_flow_enable) and (time.time() - START_TIME > 0.05 * STOP_TIME):
                    print("\033[91mNo improvement in the last 1.2 * best time, stopping the algorithm.\033[0m")
                    break
                elif not maximum_flow_enable:
                    maximum_flow_enable = True
                    print("\033[93mAllowing the algorithm to exceed the 0.8 * ub.\033[0m")
                    _, _, completed_orders, _, _ = analyze_flow(residual_graph, matrix_orders, matrix_corridors)
                    total_flow, best_iteration, current_ratio, residual_graph = reset_graph_for_max_flow(total_flow, best_iteration, current_ratio, residual_graph, matrix_corridors, matrix_orders, iteration_count, completed_orders)

            # Condições para habilitar o fluxo máximo
            if ((time.time() - START_TIME) > (0.8 * STOP_TIME) or (iteration_count > 0.8 * MAX_ITERATIONS)) and ub > 100:
                if not maximum_flow_enable:
                    maximum_flow_enable = True
                    print("\033[93mAllowing the algorithm to exceed the 0.8 * ub.\033[0m")
                    _, _, completed_orders, _, _ = analyze_flow(residual_graph, matrix_orders, matrix_corridors)
                    total_flow, best_iteration, current_ratio, residual_graph = reset_graph_for_max_flow(total_flow, best_iteration, current_ratio, residual_graph, matrix_corridors, matrix_orders, iteration_count, completed_orders)


                    if BEST_RATIO == 0:
                        print("\033[1m\033[91mNo improvement found, stopping the algorithm.\033[0m")
                        return None, None
    
        generate_log(file_path, history)
        return BEST_TOTAL_FLOW, BEST_GRAPH
    
    except KeyboardInterrupt:
        print("\nInterrupção detectada dentro do try! Salvando estado...")
        generate_log(file_path, history)
        save_all(file_path, BEST_GRAPH, matrix_orders, matrix_corridors)
        sys.exit(0)