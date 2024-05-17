// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/Counters.sol";
import "@openzeppelin/contracts/utils/structs/EnumerableSet.sol";

contract CrowdWorkingPlatform is Ownable {
    using Counters for Counters.Counter;
    using EnumerableSet for EnumerableSet.AddressSet;

    Counters.Counter private _taskIdCounter;

    constructor() Ownable(msg.sender) {
        // Additional initialization if needed
    }

    enum Role { None, User, Worker }  // Define roles for users and workers

    mapping(address => Role) public roles;  // Track roles of addresses

    struct Worker {
        address addr;
        bool isRegistered;
    }

    // Structure for tasks
    struct Task {
        uint256 id;
        address owner;  // The user who posted the task
        string description;  // Task description
        uint256 payment;  // Payment for completing the task
        string[] subtasks;  // List of subtasks
        string commonSubtask;
        mapping(uint256 => string) commonSubtaskResponses;  // Responses to the common subtask
        mapping(uint256 => string) SubtaskResults;  // Subtask results using index
        mapping(string => bool) subtaskStatus;  // Status of each subtask
        bool finalized;  // Finalization status
        string SubtaskResultsString;
    }

    mapping(address => Worker) public workers;  // Registered workers
    mapping(uint256 => Task) private tasks;  // Tasks in the platform

    event WorkerRegistered(address indexed worker);
    event UserRegistered(address indexed user);
    event TaskPosted(uint256 indexed taskId, string description, uint256 payment);
    event TaskResponseSubmitted(uint256 indexed taskId, address indexed worker, string subtaskResult);  // Declare TaskResponseSubmitted
    
    modifier onlyRegisteredUser() {
        require(roles[msg.sender] == Role.User, "Not a registered user");
        _;
    }

    modifier onlyRegisteredWorker() {
        require(roles[msg.sender] == Role.Worker, "Not a registered worker");
        _;
    }

    // Function to register as a user
    function registerUser() public {
        require(roles[msg.sender] == Role.None, "Already registered as another role");
        roles[msg.sender] = Role.User;  // Assign the User role
        emit UserRegistered(msg.sender);
    }

    // Function to register as a worker
    function registerWorker() public {
        require(roles[msg.sender] == Role.None, "Already registered as another role");
        roles[msg.sender] = Role.Worker;  // Assign the Worker role
        workers[msg.sender] = Worker(msg.sender, true);
        emit WorkerRegistered(msg.sender);
    }

    // Function to post a task (only for registered users)
    function postTask(string memory description, string[] memory subtasks) public payable onlyRegisteredUser {
        require(subtasks.length > 0, "Subtasks array cannot be empty");

        uint256 newTaskId = _taskIdCounter.current();
        _taskIdCounter.increment();

        Task storage newTask = tasks[newTaskId];
        newTask.id = newTaskId;
        newTask.owner = msg.sender;
        newTask.description = description;
        newTask.payment = msg.value;
        newTask.subtasks = subtasks;
        newTask.finalized = false;

        // Select a random index to choose the common subtask
        uint256 randomIndex = _generateRandomIndex(subtasks.length);
        newTask.commonSubtask = subtasks[randomIndex];

        for (uint256 i = 0; i < subtasks.length; i++) {
            newTask.subtaskStatus[subtasks[i]] = false;  // Initially, all subtasks are available
        }

        emit TaskPosted(newTaskId, description, newTask.payment);
    }

    // Internal function to generate a random index
    function _generateRandomIndex(uint256 maxLength) internal view returns (uint256) {
        uint256 randomNumber = uint256(keccak256(abi.encodePacked(block.timestamp, block.difficulty, block.number)));
        return randomNumber % maxLength;
    }


    // Function to get a task by ID (shows subtasks and their status)
    function getTask(uint256 taskId) public view returns (
        uint256,
        address,
        string memory,
        uint256,
        string memory,
        string[] memory,
        bool[] memory,  // Added subtask statuses
        bool
    ) {
        Task storage task = tasks[taskId];

        bool[] memory subtaskStatuses = new bool[](task.subtasks.length);  // Array to hold status of each subtask
        for (uint256 i = 0; i < task.subtasks.length; i++) {
            subtaskStatuses[i] = task.subtaskStatus[task.subtasks[i]];  // True if subtask is completed, false otherwise
        }

        return (
            task.id,
            task.owner,
            task.description,
            task.payment,
            task.commonSubtask,
            task.subtasks,  // List of subtasks
            subtaskStatuses,  // Status of each subtask
            task.finalized  // Whether the task is finalized
        );
    }
    
    // Function to submit a response to a task's subtasks
    function submitTaskResponse(uint256 taskId, uint256 subtaskIndex, string memory commonSubtaskResponse, string memory subtaskResult)
        public 
        onlyRegisteredWorker 
    {
        Task storage task = tasks[taskId];
        require(!task.finalized, "Task already finalized");
        require(subtaskIndex < task.subtasks.length, "Invalid subtask index");

        // Update subtask status based on the worker's response
        string memory subtask = task.subtasks[subtaskIndex];
        require(!task.subtaskStatus[subtask], "Subtask already answered");
        task.subtaskStatus[subtask] = true;  // Mark the subtask as completed
        task.commonSubtaskResponses[subtaskIndex] = commonSubtaskResponse;
        task.SubtaskResults[subtaskIndex] = subtaskResult;  // Store the worker's subtask result

        emit TaskResponseSubmitted(taskId, msg.sender, subtaskResult);  // Emit the event
    }
    mapping(string => uint256) responseCount;
    // Function to validate the subtasks of a task and finalize the result
    function validateSubtasks(uint256 taskId) public onlyOwner {
        Task storage task = tasks[taskId];
        require(!task.finalized, "Task already finalized");

        // Check if all subtasks have been answered
        for (uint256 i = 0; i < task.subtasks.length; i++) {
            string memory subtask = task.subtasks[i];
            require(task.subtaskStatus[subtask], "Not all subtasks answered");
        }

        // Ensure responseCount mapping is initialized properly
        require(responseCount[task.commonSubtask] == 0, "Response count already initialized");

        string memory majorityResponse;
        uint256 maxCount = 0;

        // Initialize response count for each response
        for (uint256 i = 0; i < task.subtasks.length; i++) {
            string memory response = task.commonSubtaskResponses[i];
            responseCount[response]++;
            // Find the majority response
            if (responseCount[response] > maxCount) {
                majorityResponse = response;
                maxCount = responseCount[response];
            }
        }

        // Check if there are responses different from the majority response
        bool differentResponses = false;
        for (uint256 i = 0; i < task.subtasks.length; i++) {
            string memory response = task.commonSubtaskResponses[i];
            if (keccak256(abi.encodePacked(response)) != keccak256(abi.encodePacked(majorityResponse))) {
                // Remove responses different from the majority response
                task.commonSubtaskResponses[i] = "";
                task.SubtaskResults[i] = "";
                task.subtaskStatus[task.subtasks[i]] = false; // Reset subtask status
                differentResponses = true;
            }
        }

        // Finalize the result if there are no responses different from the majority response
        if (!differentResponses) {
            task.finalized = true;

            // Store the subtask results string
            string memory subtaskResultsString;
            for (uint256 i = 0; i < task.subtasks.length; i++) {
                if (bytes(task.commonSubtaskResponses[i]).length > 0) {
                    subtaskResultsString = string(abi.encodePacked(subtaskResultsString, task.SubtaskResults[i], "\n"));
                }
            }
            task.SubtaskResultsString = subtaskResultsString;
        }
    }
    function getNonFinalizedTasks() public view onlyRegisteredWorker returns (uint256[] memory) {
        uint256[] memory nonFinalizedTaskIds = new uint256[](_taskIdCounter.current());
        uint256 count = 0;

        // Loop through all tasks and check if they are finalized
        for (uint256 i = 0; i < _taskIdCounter.current(); i++) {
            Task storage task = tasks[i];
            if (!task.finalized) {
                nonFinalizedTaskIds[count] = task.id;
                count++;
            }
        }

        // Create a new array to store the non-finalized task IDs
        uint256[] memory result = new uint256[](count);
        for (uint256 i = 0; i < count; i++) {
            result[i] = nonFinalizedTaskIds[i];
        }

        return result;
    }

    // Function to get the result string of a finalized task
    function getTaskResultString(uint256 taskId) public view returns (string memory) {
        Task storage task = tasks[taskId];
        require(task.finalized, "Task is not finalized");
        require(task.owner == msg.sender, "Only the owner can view the result");

        return task.SubtaskResultsString;
    }

}
