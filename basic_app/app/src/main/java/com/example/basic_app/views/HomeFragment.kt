package com.example.basic_app.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.basic_app.viewmodels.MainViewModel
import com.example.basic_app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private val mainViewModel: MainViewModel by activityViewModels()
    //private val homeViewModel: HomeViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.mainViewModel = mainViewModel
        binding.lifecycleOwner = this

        binding.ledOff.setOnClickListener{ mainViewModel.setLed(0) }
        binding.ledOn.setOnClickListener{ mainViewModel.setLed(1) }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}