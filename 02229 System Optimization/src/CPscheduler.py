import collections
from ortools.sat.python import cp_model
from CPdataHandler import *


def main():
    """Minimal jobshop problem."""
    # Data.
    filepath = 'dataBase\\inf_10_10\\taskset__1643188013-a_0.1-b_0.1-n_30-m_20-d_unif-p_2000-q_4000-g_1000-t_5__0__tsk.csv'
    jobs_data = CPDataHandler(filepath)
    machines_count = 1
    all_machines = range(machines_count)
    # Computes horizon dynamically as the sum of all durations.
    horizon = 12000 # Hyperperiod

    # Create the model.
    model = cp_model.CpModel()

    # Named tuple to store information about created variables.
    task_type = collections.namedtuple('task_type', 'start end interval priority deadline start_val deadline_val')
    # Named tuple to manipulate solution information.
    assigned_task_type = collections.namedtuple('assigned_task_type',
                                                'start job index duration')

    # Creates job intervals and add to the corresponding machine lists.
    all_tasks = {}
    machine_to_intervals = collections.defaultdict(list)

    for job_id, job in enumerate(jobs_data):
        for task_id, task in enumerate(job):
            machine = 0
            duration = task[1]
            start_time = task[0]
            priority = task[3]
            deadline_val = task[2]
            suffix = '_%i_%i' % (job_id, task_id)
            start_var = model.NewIntVar(start_time, horizon, 'start' + suffix) 
            end_var = model.NewIntVar(0, deadline_val, 'end' + suffix)
            deadline_var = model.NewIntVar(0, horizon, 'deadline' + suffix)
            interval_var = model.NewIntervalVar(start_var, duration, end_var,
                                                'interval' + suffix)
            all_tasks[job_id, task_id] = task_type(start=start_var,
                                                   end=end_var,
                                                   interval=interval_var,
                                                   priority=priority,
                                                   deadline=deadline_var,
                                                   start_val = start_time,
                                                   deadline_val = deadline_val
                                                   )
            machine_to_intervals[machine].append(interval_var)
            
    # Create and add disjunctive constraints.
    for machine in all_machines:
        model.AddNoOverlap(machine_to_intervals[machine])

    # Precedences inside a job.
    for job_id, job in enumerate(jobs_data):

        for task_id in range(len(job) - 1):
            model.Add(all_tasks[job_id, task_id +
                                1].start >= all_tasks[job_id, task_id].end)
            for x, job_comp in enumerate(jobs_data):

                for y in range(len(job_comp)):
                    if x==job_id and y==task_id:
                        continue
                    if all_tasks[job_id, task_id].priority < all_tasks[x, y].priority and all_tasks[job_id, task_id].start_val >= all_tasks[x, y].start_val:
                        model.Add(all_tasks[job_id, task_id].start >= all_tasks[x, y].start)
    

    # Makespan objective.
    obj_var = model.NewIntVar(0, horizon, 'makespan')
    model.AddMaxEquality(obj_var, [
        all_tasks[job_id, len(job) - 1].end
        for job_id, job in enumerate(jobs_data)
    ])
    model.Minimize(obj_var)

    # Creates the solver and solve.
    solver = cp_model.CpSolver()
    status = solver.Solve(model)

    if status == cp_model.OPTIMAL or status == cp_model.FEASIBLE:
        total_tasks = 0
        print('Solution:')
        # Create one list of assigned tasks per machine.
        assigned_jobs = collections.defaultdict(list)
        for job_id, job in enumerate(jobs_data):
            for task_id, task in enumerate(job):
                machine = 0 # Only one machine used
                assigned_jobs[machine].append(
                    assigned_task_type(start=solver.Value(
                        all_tasks[job_id, task_id].start),
                                       job=job_id,
                                       index=task_id,
                                       duration=task[1]))

        # Create per machine output lines.
        output = ''
        for machine in all_machines:
            # Sort by starting time.
            assigned_jobs[machine].sort()
            sol_line_tasks = 'Machine ' + str(machine) + ': '
            sol_line = '           '
            sol_line_tuple = ''
            for assigned_task in assigned_jobs[machine]:
                name = 'job_%i_task_%i' % (assigned_task.job,
                                           assigned_task.index)
                # Add spaces to output to align columns.
                sol_line_tasks += '%-15s' % name

                start = assigned_task.start
                duration = assigned_task.duration
                sol_tmp = '[%i,%i]' % (start, start + duration)
                # Add spaces to output to align columns.
                sol_line += '%-15s' % sol_tmp
                sol_line_tuple += ''.join(map(str, (name,sol_tmp))) + '\n'
                total_tasks += 1
            sol_line += '\n'
            sol_line_tasks += '\n'
            output = sol_line_tuple

        # Finally print the solution found.
        print(f'Optimal Schedule Length: {solver.ObjectiveValue()}')
        print(output + '\n')
        print(f'amount of jobs: {len(jobs_data)}')
        print(f'total tasks: {total_tasks}')
    else:
        print('No solution found.')
 
    # Statistics.
    print('\nStatistics')
    print('  - conflicts: %i' % solver.NumConflicts())
    print('  - branches : %i' % solver.NumBranches())
    print('  - wall time: %f s' % solver.WallTime())


if __name__ == '__main__':
    main()
